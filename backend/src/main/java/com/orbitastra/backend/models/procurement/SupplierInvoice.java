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
import com.orbitastra.backend.models.procurement.embedded.SupplierInvoiceLine;
import com.orbitastra.backend.models.procurement.enums.SupplierInvoiceStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A bill the vendor has sent, and what the school owes on it.
 *
 * <p>This is the mirror of FeeInvoice. A fee invoice is money the school is owed; this is
 * money the school owes. Until this model existed, `finance` was entirely money coming in —
 * billing, payments, concessions, aid, wallets — and the only money going out anywhere in
 * the system was payroll. The school was buying rice every week with nothing recording that
 * it owed anybody for it.
 *
 * <p>The bill and the goods arrive on different days and are two separate records for that
 * reason. A GoodsReceipt is what turned up; this is the paperwork asking to be paid for it.
 * One bill can cover several deliveries, which is why {@code goodsReceiptDocsIds} is a list:
 * a vendor delivering vegetables every morning sends one bill at the end of the month.
 *
 * <p>The lines are what the **vendor claims**, not what the school agreed, and they carry
 * the ordered rate and the accepted quantity beside the billed ones. That comparison is the
 * single most valuable thing in this package. A vendor billing 63 a kilogram against an
 * order at 61.50, or billing for two hundred kilograms when fifteen went back damp, is not
 * caught by looking at the bill total — the vendor added their bill up correctly. It is only
 * caught line by line.
 *
 * <p>VERIFIED and APPROVED are two states because they are two people doing two jobs.
 * Verifying is clerical: do these figures match the order and the delivery? Approving is
 * authority: yes, pay it. Collapsing them means whoever checks the arithmetic also releases
 * the money, and that is the arrangement every audit asks about.
 *
 * <p>DISPUTED is the state a simpler design leaves out, and its absence is expensive. A bill
 * the school is refusing to pay is not overdue and is not paid either. With nowhere to put
 * it, it sits in the payables list ageing quietly and turns up in a report as money owed
 * when the school has said it does not owe it.
 *
 * <p>{@code amountPaid} and {@code outstandingAmount} are running totals kept so a payables
 * list loads without opening every payment. VendorPayment and its allocations remain the
 * real record, and both figures must always be rebuildable from them — the same rule
 * FeeInvoice follows for {@code allocatedPaymentTotal}.
 *
 * <p>{@code taxDeductedAmount} is tax withheld from the payment, which the school pays to
 * the authorities instead of to the vendor. It is here because it changes what actually
 * leaves the bank, which is arithmetic the school cannot do without. **The platform files
 * nothing and issues no certificates**; see the README.
 *
 * <p>{@code dueDate} is worked out from the bill date and the order's payment terms, and
 * then stored, because it is what a payables list sorts on and a stored date does not have
 * to be recomputed for every row.
 *
 * <p>The service checks that the vendor's own invoice number is not entered twice for the
 * same vendor, that the line totals add up, that a bill is not approved while a variance is
 * unexplained, that a dispute carries a reason, that the paid and outstanding figures agree
 * with the allocations against it, and that a bill with payments against it is disputed or
 * credited rather than deleted.
 */
@Document(collection = "supplier_invoices")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_supplier_invoice_no_uniq",
                def = "{'schoolId': 1, 'invoiceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vendor_invoice_no_uniq",
                def = "{'schoolId': 1, 'vendorDocsId': 1, 'vendorInvoiceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_supplier_invoice_status_due_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueDate': 1}"),
        @CompoundIndex(
                name = "school_supplier_invoice_vendor_idx",
                def = "{'schoolId': 1, 'vendorDocsId': 1, 'invoiceDate': -1}"),
        @CompoundIndex(
                name = "school_supplier_invoice_order_idx",
                def = "{'schoolId': 1, 'purchaseOrderDocsIds': 1}"),
        @CompoundIndex(
                name = "school_supplier_invoice_receipt_idx",
                def = "{'schoolId': 1, 'goodsReceiptDocsIds': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoice extends SchoolBase {

    // The school's own number for this bill, from NumberSequence type SUPPLIER_INVOICE.
    // The school needs its own because two vendors can easily use the same numbering.
    // Example: "SINV/2026/000112"
    @NotBlank
    private String invoiceNo;

    // The number the vendor put on their own bill, which is what they will quote when
    // they ring to ask about it. Unique per vendor, so the same bill cannot be entered
    // and paid twice. Example: "ST/2026/4471"
    @NotBlank
    private String vendorInvoiceNo;

    // The date on the vendor's bill, which is what payment terms run from and is not
    // the day it reached the school. Example: 2026-08-21
    @NotNull
    private LocalDate vendorInvoiceDate;

    // The day the school actually received the bill. Kept separately because a bill
    // dated the first and delivered on the twentieth is a vendor shortening the credit
    // period, and the gap is worth being able to see. Example: 2026-08-24
    private LocalDate receivedOn;

    // Links to Vendor.id. Example: "67bd1122dc3f7d0011223344"
    @NotBlank
    private String vendorDocsId;

    // Links to every PurchaseOrder.id this bill is against.
    // Example: ["67bd1126dc3f7d0055667788"]
    @Builder.Default
    private List<String> purchaseOrderDocsIds = new ArrayList<>();

    // Links to every GoodsReceipt.id this bill is against. A list because a vendor
    // delivering daily sends one bill at the end of the month.
    // Example: ["67bd1128dc3f7d0077889900"]
    @Builder.Default
    private List<String> goodsReceiptDocsIds = new ArrayList<>();

    // What the vendor is charging for, with the agreed figures beside the billed ones.
    @Valid
    @Builder.Default
    private List<SupplierInvoiceLine> lines = new ArrayList<>();

    // The line amounts before tax, as billed. Example: 12600.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotalAmount;

    // Tax the vendor has charged. Example: 630.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;

    // Freight, loading and packing, as billed. Example: 250.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal otherChargesAmount;

    // What the vendor says the school owes. Example: 13480.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal billedTotalAmount;

    // What the school has agreed to pay, once any variance has been argued out. Equal to
    // the billed total in the ordinary case, and the figure everything downstream uses.
    // Example: 12000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal agreedTotalAmount;

    // Tax withheld from the payment and paid to the authorities instead of the vendor.
    // Here because it changes what leaves the bank. Example: 120.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxDeductedAmount;

    // What has been paid so far. A running total, rebuildable from the allocations on
    // VendorPayment. Example: 12000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    // The agreed total, less what has been paid and less tax withheld. A running total,
    // rebuildable the same way. Example: 0.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal outstandingAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // When the money has to reach the vendor, worked out from the bill date and the
    // order's payment terms, and stored because a payables list sorts on it.
    // Example: 2026-09-20
    @NotNull
    private LocalDate dueDate;

    // Where the bill has got to. Example: SupplierInvoiceStatus.PAID
    @NotNull
    @Builder.Default
    private SupplierInvoiceStatus status = SupplierInvoiceStatus.RECEIVED;

    // Links to Staff.id of whoever checked the figures against the order and delivery.
    // Example: "67aa15d9dc3f7d0044444444"
    private String verifiedByStaffDocsId;

    // When they did. Example: 2026-08-25T05:00:00Z
    private Instant verifiedAt;

    // Links to Staff.id of whoever cleared it for payment. Must not be the same person
    // who verified it. Example: "67aa15d9dc3f7d0055555555"
    private String approvedByStaffDocsId;

    // When they did. Example: 2026-08-26T04:30:00Z
    private Instant approvedAt;

    // Why the school is refusing to pay. Required for DISPUTED.
    // Example: "Billed 200 kg; 185 kg accepted, 15 kg returned damp on 20 August."
    private String disputeReason;

    // Why the bill was withdrawn. Required for CANCELLED.
    // Example: "Vendor cancelled it and reissued as ST/2026/4488."
    private String cancellationReason;

    // Links to DocumentRecord.id for the scan of the bill itself.
    // Example: "67bd1129dc3f7d0088990011"
    private String invoiceDocumentDocsId;

    // Anything worth knowing.
    // Example: "Rate difference settled by telephone. Vendor accepted the order rate."
    private String remarks;
}
