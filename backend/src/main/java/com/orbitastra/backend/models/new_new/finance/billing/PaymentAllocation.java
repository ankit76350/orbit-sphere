package com.orbitastra.backend.models.new_new.finance.billing;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.AllocationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Records that a set amount of one FeePayment was put towards one FeeInvoice.
 *
 * <p>This is the record that joins money to bills. Without it, one payment
 * covering three invoices, or three payments slowly clearing one invoice, cannot
 * be explained. It is also what makes {@code FeeInvoice.allocatedPaymentTotal}
 * rebuildable instead of merely believed.
 *
 * <p>Allocations are never edited or deleted. Undoing one means writing a second
 * allocation with status REVERSAL that points back through
 * {@code reversalOfAllocationDocsId}, and marking the original REVERSED. Both
 * rows stay, so the history reads in order.
 *
 * <p>{@code allocationSequence} counts attempts on the same payment and invoice
 * pair. It is part of the unique key so a reversal followed by a fresh allocation
 * of the same money does not clash with the row it replaced.
 *
 * <p>The service checks that the sum of active allocations never goes past either
 * the payment amount or the invoice's outstanding amount, that both sides belong
 * to the same school, and that only a SUCCEEDED payment is allocated.
 */
@Document(collection = "payment_allocations")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_payment_invoice_allocation_uniq",
                def = "{'schoolId': 1, 'feePaymentDocsId': 1, 'feeInvoiceDocsId': 1, 'allocationSequence': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_invoice_allocation_status_idx",
                def = "{'schoolId': 1, 'feeInvoiceDocsId': 1, 'status': 1, 'allocatedAt': 1}"),
        @CompoundIndex(
                name = "school_payment_allocation_status_idx",
                def = "{'schoolId': 1, 'feePaymentDocsId': 1, 'status': 1, 'allocatedAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation extends SchoolBase {

    // Links to FeePayment.id. Example: "67ae1122dc3f7d0011223344"
    @NotBlank
    private String feePaymentDocsId;

    // Links to FeeInvoice.id. Example: "67ae2233dc3f7d0022334455"
    @NotBlank
    private String feeInvoiceDocsId;

    // Links to Student.id, copied in so student statements need no join.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // Counts attempts on the same payment and invoice pair, starting at 1.
    // Example: 1
    @NotNull
    @Builder.Default
    private Integer allocationSequence = 1;

    // Money put towards this invoice. Example: 5000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: AllocationStatus.ACTIVE
    @NotNull
    @Builder.Default
    private AllocationStatus status = AllocationStatus.ACTIVE;

    // Example: 2026-04-08T10:15:00Z
    @NotNull
    private Instant allocatedAt;

    // Links to the staff identity that placed the money, or null when the
    // system placed it automatically against the oldest unpaid invoice.
    // Example: "67aa15d9dc3f7d0044444444"
    private String allocatedByDocsId;

    // Set on the original row once a reversal cancels it.
    // Example: "67ae3344dc3f7d0033445566"
    private String reversedByAllocationDocsId;

    // Set on a reversal row and points at the allocation being cancelled.
    // Example: "67ae4455dc3f7d0044556677"
    private String reversalOfAllocationDocsId;

    // Why the allocation was undone.
    // Example: "Money moved to the correct invoice after a parent query."
    private String reversalReason;

    // Links to JournalEntry.id posted for this allocation.
    // Example: "67ae5566dc3f7d0055667788"
    private String journalEntryDocsId;
}
