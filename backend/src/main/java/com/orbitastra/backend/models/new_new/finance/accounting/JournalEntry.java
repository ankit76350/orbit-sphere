package com.orbitastra.backend.models.new_new.finance.accounting;

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

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.accounting.embedded.JournalLine;
import com.orbitastra.backend.models.new_new.finance.enums.JournalEntryStatus;
import com.orbitastra.backend.models.new_new.finance.enums.JournalSourceType;

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
 * One set of debits and credits written into the school's books.
 *
 * <p>Once the status is POSTED the lines must never be touched again. A mistake
 * is fixed by posting a second entry that reverses the first and points back at
 * it through {@code reversalOfJournalDocsId}. This is the rule the whole finance
 * module leans on: fee invoices, payments and wallet movements all end up here,
 * and none of them may quietly rewrite history.
 *
 * <p>{@code sourceType}, {@code sourceDocsId} and {@code idempotencyKey} together
 * make sure one business event posts to the books exactly once, even if a webhook
 * arrives twice or a batch job is run again. The unique index on those fields is
 * what enforces it.
 *
 * <p>{@code totalDebit} and {@code totalCredit} are saved on the entry so a
 * trial balance does not have to add up every line. The service checks they are
 * equal before allowing a post; an entry that does not balance is never saved as
 * POSTED.
 *
 * <p>The service also checks that the fiscal period is open, that the accounting
 * date falls inside that period, and that a posting account has
 * {@code postingAllowed} set to true.
 */
@Document(collection = "journal_entries")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_journal_no_uniq",
                def = "{'schoolId': 1, 'journalNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_journal_source_idempotency_uniq",
                def = "{'schoolId': 1, 'sourceType': 1, 'sourceDocsId': 1, 'idempotencyKey': 1}",
                unique = true,
                partialFilter = "{'idempotencyKey': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_journal_period_status_posted_idx",
                def = "{'schoolId': 1, 'fiscalPeriodDocsId': 1, 'status': 1, 'postedAt': -1}"),
        @CompoundIndex(
                name = "school_journal_source_lookup_idx",
                def = "{'schoolId': 1, 'sourceType': 1, 'sourceDocsId': 1}"),
        @CompoundIndex(
                name = "school_journal_accounting_date_idx",
                def = "{'schoolId': 1, 'accountingDate': -1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry extends SchoolBase {

    // School-scoped number from NumberSequence type JOURNAL_ENTRY.
    // Example: "JV/2026/000731"
    @NotBlank
    private String journalNo;

    // Links to FiscalPeriod.id this entry is posted into.
    // Example: "67ac9911dc3f7d0011223344"
    @NotBlank
    private String fiscalPeriodDocsId;

    // Date the entry counts from in the books. Example: 2026-04-01
    @NotNull
    private LocalDate accountingDate;

    // Plain description of what happened.
    // Example: "April 2026 tuition invoices raised for class V."
    @NotBlank
    private String description;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: JournalEntryStatus.POSTED
    @NotNull
    @Builder.Default
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;

    // Which finance record caused this entry.
    // Example: JournalSourceType.FEE_INVOICE
    @NotNull
    private JournalSourceType sourceType;

    // Links to the record named by sourceType. Null for a manual entry.
    // Example: "67ae2233dc3f7d0022334455"
    private String sourceDocsId;

    // Stops the same event from posting to the books twice.
    // Example: "fee-invoice-issued-67ae2233dc3f7d0022334455"
    private String idempotencyKey;

    // The debits and credits, which must add up to the same total.
    @Valid
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    // Sum of the debit side. Example: 11350.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    // Sum of the credit side, which must equal totalDebit. Example: 11350.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    // Links to the staff identity that prepared the entry.
    // Example: "67aa15d9dc3f7d0044444444"
    private String preparedByDocsId;

    // Links to the staff identity that approved it, where approval is needed.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Links to the staff identity that posted it to the books.
    // Example: "67aa15d9dc3f7d0055555555"
    private String postedByDocsId;

    // Example: 2026-04-01T05:00:00Z
    private Instant postedAt;

    // Set on a reversing entry and points at the entry being cancelled.
    // Example: "67aa9911dc3f7d0022334455"
    private String reversalOfJournalDocsId;

    // Set on the original entry once a reversal cancels it.
    // Example: "67aa9911dc3f7d0033445566"
    private String reversedByJournalDocsId;

    // Why the entry was reversed.
    // Example: "Invoices were raised against the wrong class."
    private String reversalReason;

    // Why the entry was turned down before it reached the books.
    // Example: "Wrong income account chosen on line 2."
    private String rejectionReason;

    // Links to DocumentRecord.id for the supporting paperwork.
    // Example: "67ad3344dc3f7d0033445566"
    private String supportingDocumentDocsId;
}
