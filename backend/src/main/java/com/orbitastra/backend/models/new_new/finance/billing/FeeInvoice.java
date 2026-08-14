package com.orbitastra.backend.models.new_new.finance.billing;

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

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.finance.billing.embedded.FeeInvoiceLine;
import com.orbitastra.backend.models.new_new.finance.enums.ChargeSourceType;
import com.orbitastra.backend.models.new_new.finance.enums.FeeInvoiceStatus;

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
 * One bill given to a family for one student, for one installment or one-off
 * charge.
 *
 * <p>An issued invoice is a financial record. It is never edited to a smaller
 * amount and never deleted. A billing mistake is fixed either by voiding the
 * invoice before any payment lands on it, or by issuing a reversing invoice that
 * points back at it through {@code reversalOfInvoiceDocsId}.
 *
 * <p>Money never lands on the invoice directly. A FeePayment is received, and
 * one PaymentAllocation per invoice records how much of that payment went where.
 * {@code allocatedPaymentTotal} and {@code outstandingAmount} are running totals
 * kept so lists and defaulter reports load fast; the allocations remain the real
 * record and the totals must always be rebuildable from them.
 *
 * <p>{@code invoiceNo} is unique inside one school, not across the whole
 * database, and comes from NumberSequence with type FEE_INVOICE. Two schools may
 * both have INV/2026/000001.
 *
 * <p>Not every invoice comes from a fee structure. {@code sourceType} and
 * {@code sourceDocsId} let a hostel stay, a library fine or a trip raise a bill
 * through this same collection, so those areas do not each need billing models
 * of their own.
 *
 * <p>The service checks that the line totals add up to the header totals, that
 * the invoice date is not after the due date, that a void carries a reason, and
 * that an invoice with allocations against it is reversed rather than voided.
 */
@Document(collection = "fee_invoices")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_fee_invoice_no_uniq",
                def = "{'schoolId': 1, 'invoiceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_installment_invoice_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'feeStructureDocsId': 1, 'installmentNo': 1, 'generationSequence': 1}",
                unique = true,
                partialFilter = "{'feeStructureDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_year_student_invoice_status_due_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'status': 1, 'dueDate': 1}"),
        @CompoundIndex(
                name = "school_year_invoice_status_due_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'dueDate': 1}"),
        @CompoundIndex(
                name = "school_invoice_source_idx",
                def = "{'schoolId': 1, 'sourceType': 1, 'sourceDocsId': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeInvoice extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type FEE_INVOICE.
    // Example: "INV/2026/000578"
    @NotBlank
    private String invoiceNo;

    // Links to Guardian.id for the person the bill is addressed to.
    // Example: "67aa15d9dc3f7d0066666666"
    private String guardianDocsId;

    // Links to SchoolClass.id, saved as it was on the billing date so an old
    // bill still shows the right class after the student moves up.
    // Example: "67ab3322dc3f7d0044556677"
    private String classDocsId;

    // Section the student was in on the billing date. Example: "A"
    private String sectionNo;

    // Links to FeeStructure.id this bill came from. Null for a one-off charge.
    // Example: "67ac3344dc3f7d0077889900"
    private String feeStructureDocsId;

    // Installment inside that structure. Null for a one-off charge. Example: 1
    private Integer installmentNo;

    // Counts how many times a bill has been raised for the same student and
    // installment, starting at 1. It goes up when an earlier bill was voided or
    // reversed and a fresh one has to be raised, so the correction does not clash
    // with the record it replaces. Example: 1
    @NotNull
    @Builder.Default
    private Integer generationSequence = 1;

    // Links to AcademicTerm.id when the installment belongs to a term.
    // Example: "67ab5511dc3f7d0099887766"
    private String termDocsId;

    // What caused this bill. Example: ChargeSourceType.FEE_STRUCTURE
    @NotNull
    private ChargeSourceType sourceType;

    // Links to the record named by sourceType. Null for FEE_STRUCTURE and MANUAL.
    // Example: "67ad1122dc3f7d0011223344"
    private String sourceDocsId;

    // Date the bill is dated. Example: 2026-04-01
    @NotNull
    private LocalDate invoiceDate;

    // Date the money has to reach the school by. Example: 2026-04-10
    @NotNull
    private LocalDate dueDate;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: FeeInvoiceStatus.PARTIALLY_PAID
    @NotNull
    @Builder.Default
    private FeeInvoiceStatus status = FeeInvoiceStatus.DRAFT;

    // Sum of the lines before discount and tax. Example: 15000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subTotal;

    // Total taken off by concessions and awards. Example: 3750.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    // Total tax on the lines. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    // Late-payment charge added to this bill. Example: 100.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lateFeeTotal = BigDecimal.ZERO;

    // What the family owes in total. Example: 11350.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal grandTotal;

    // Sum of the active allocations pointing here. Example: 5000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal allocatedPaymentTotal = BigDecimal.ZERO;

    // Amount the school has given up on collecting. Example: 0.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal writtenOffTotal = BigDecimal.ZERO;

    // grandTotal minus what is paid and written off. Example: 6350.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal outstandingAmount;

    // What is being charged.
    @Valid
    @Builder.Default
    private List<FeeInvoiceLine> lines = new ArrayList<>();

    // Links to DocumentRecord.id for the printed or emailed bill.
    // Example: "67ad3344dc3f7d0033445566"
    private String invoiceDocumentDocsId;

    // Set on a reversing invoice and points at the invoice being cancelled.
    // Example: "67ad4455dc3f7d0044556677"
    private String reversalOfInvoiceDocsId;

    // Example: 2026-04-01T04:30:00Z
    private Instant issuedAt;

    // Example: 2026-06-30T11:00:00Z
    private Instant paidAt;

    // Example: 2026-05-05T07:15:00Z
    private Instant voidedAt;

    // Links to the staff identity that voided the invoice.
    // Example: "67aa15d9dc3f7d0044444444"
    private String voidedByDocsId;

    // Needed whenever the status becomes VOID or WRITTEN_OFF.
    // Example: "Duplicate bill created during the April run."
    private String voidReason;

    // Note printed on the bill for the family.
    // Example: "Please quote the invoice number when paying online."
    private String remarks;
}
