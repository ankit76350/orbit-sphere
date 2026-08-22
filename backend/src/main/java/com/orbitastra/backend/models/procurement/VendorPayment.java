package com.orbitastra.backend.models.procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.common.enums.PaymentMode;
import com.orbitastra.backend.models.procurement.embedded.VendorPaymentAllocation;
import com.orbitastra.backend.models.procurement.enums.VendorPaymentStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Money actually leaving the school and reaching a vendor.
 *
 * <p>This is the sixth model in a package I described as five, and it is here because
 * without it SupplierInvoice.amountPaid is a number somebody types in with nothing behind
 * it. Every running total elsewhere in this system has to be rebuildable from the records
 * that caused it, and a paid figure with no payment record breaks that rule at the one place
 * it matters most — the place where money leaves.
 *
 * <p>It is the mirror of FeePayment. A fee payment is money arriving from a family; this is
 * money going out to a supplier. The shape is the same on purpose: one payment, allocated
 * across several bills, because a school pays its rice supplier once a month and that single
 * transfer settles four bills. The allocations say which four, which is the only way the
 * payables list can say what is still open.
 *
 * <p>The allocations are **embedded** rather than a collection, and that is the one
 * deliberate difference from the fees side. A vendor payment settles a handful of bills and
 * they are always read together with the payment. A fee allocation is queried constantly
 * from the invoice direction by hundreds of parents at once and has to be a document. Here
 * the traffic runs the other way, so an index into the array does the job.
 *
 * <p>INITIATED and COMPLETED are separate states because a cheque written on Monday and
 * cleared on Thursday is money the school has committed but not yet lost. Treating the two
 * as one moment makes the school's own bank figure wrong for three days, and a school
 * running close to its overdraft cares about exactly those three days.
 *
 * <p>FAILED and CANCELLED are both endings and mean different things. FAILED is the bank
 * refusing — the transfer bounced, the cheque came back. CANCELLED is the school stopping it
 * before the money went. A failure needs somebody to try again; a cancellation does not, and
 * a single state for both leaves nobody knowing which.
 *
 * <p>{@code bankAccountDocsId} says which of the school's own accounts the money left, so
 * this can be tied to a bank statement. Without it a payables ledger and a bank statement
 * can never be reconciled, which is the first thing an auditor tries to do.
 *
 * <p>The service checks that the allocations add up to the payment amount, that no
 * allocation exceeds what is outstanding on the bill it names, that a payment is not made
 * against an unapproved or disputed bill, that a failure or cancellation carries a reason,
 * and that completing a payment updates the paid and outstanding figures on every bill it
 * touches in one operation.
 */
@Document(collection = "vendor_payments")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_vendor_payment_no_uniq",
                def = "{'schoolId': 1, 'paymentNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vendor_payment_vendor_idx",
                def = "{'schoolId': 1, 'vendorDocsId': 1, 'paymentDate': -1}"),
        @CompoundIndex(
                name = "school_vendor_payment_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'paymentDate': -1}"),
        @CompoundIndex(
                name = "school_vendor_payment_invoice_idx",
                def = "{'schoolId': 1, 'allocations.supplierInvoiceDocsId': 1}"),
        @CompoundIndex(
                name = "school_vendor_payment_bank_idx",
                def = "{'schoolId': 1, 'bankAccountDocsId': 1, 'paymentDate': -1}"),
        @CompoundIndex(
                name = "school_vendor_payment_reference_idx",
                def = "{'schoolId': 1, 'transactionReference': 1}",
                partialFilter = "{'transactionReference': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPayment extends SchoolBase {

    // School-scoped number from NumberSequence type VENDOR_PAYMENT.
    // Example: "VP/2026/000087"
    @NotBlank
    private String paymentNo;

    // Links to Vendor.id being paid. One vendor per payment, because a payment goes into
    // one bank account. Example: "67bd1122dc3f7d0011223344"
    @NotBlank
    private String vendorDocsId;

    // The day the payment was made. Example: 2026-09-18
    @NotNull
    private LocalDate paymentDate;

    // How much left the school in total. Example: 12000.00
    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // How it was paid. Reuses the same list as fee payments, because cash is cash
    // whichever direction it moves. Example: PaymentMode.BANK_TRANSFER
    @NotNull
    private PaymentMode paymentMode;

    // Links to BankAccount.id for the school's own account the money left, so this can
    // be matched against a bank statement. Example: "67ae1122dc3f7d0011223344"
    private String bankAccountDocsId;

    // The bank's own reference for the transfer, or the cheque number. What somebody
    // quotes when the vendor says the money never arrived.
    // Example: "NEFT/HDFC/2026091800471"
    private String transactionReference;

    // The date on the cheque, which is not always the day it was written. A
    // post-dated cheque is a promise for a future date, and this is that date.
    // Example: 2026-09-25
    private LocalDate instrumentDate;

    // Which bills this payment settles, and how much went against each. At least one,
    // because a payment against nothing cannot be reconciled with anything.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<VendorPaymentAllocation> allocations = new ArrayList<>();

    // Whether the money actually got there. Example: VendorPaymentStatus.COMPLETED
    @NotNull
    @Builder.Default
    private VendorPaymentStatus status = VendorPaymentStatus.INITIATED;

    // When it cleared. Null until it does, and that null is what says the money has
    // been committed but not yet lost. Example: 2026-09-21T06:00:00Z
    private Instant completedAt;

    // Why the bank refused it. Required for FAILED.
    // Example: "Returned by the bank: account name did not match."
    private String failureReason;

    // Why the school stopped it. Required for CANCELLED.
    // Example: "Cheque stopped after the rate dispute was reopened."
    private String cancellationReason;

    // Links to Staff.id of whoever released the payment.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String paidByStaffDocsId;

    // Links to DocumentRecord.id for the transfer advice or the cheque counterfoil.
    // Example: "67bd1130dc3f7d0099001122"
    private String paymentDocumentDocsId;

    // Anything worth knowing.
    // Example: "Paid short by 1480 pending the rate difference on ST/2026/4471."
    private String remarks;
}
