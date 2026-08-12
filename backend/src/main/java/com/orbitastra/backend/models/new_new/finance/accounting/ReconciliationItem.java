package com.orbitastra.backend.models.new_new.finance.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.LedgerEntryDirection;
import com.orbitastra.backend.models.new_new.finance.enums.ReconciliationMatchState;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One line from a bank statement, and what it was matched to in the books.
 *
 * <p>Items are their own collection, not a list inside the run, because a monthly
 * statement for a school can run into thousands of lines and each one is worked
 * on separately.
 *
 * <p>{@code statementLineKey} is built from the bank's own values for the line so
 * loading the same statement twice cannot create the line twice. The unique index
 * on the run and this key is what enforces it.
 *
 * <p>A suggested match is never treated as done. {@code confidenceScore} records
 * how sure the automatic matching was, and a person still has to move the item to
 * MATCHED.
 */
@Document(collection = "reconciliation_items")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_run_statement_line_uniq",
                def = "{'schoolId': 1, 'reconciliationRunDocsId': 1, 'statementLineKey': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_run_match_state_idx",
                def = "{'schoolId': 1, 'reconciliationRunDocsId': 1, 'matchState': 1, 'valueDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItem extends SchoolBase {

    // Links to ReconciliationRun.id. Example: "67afaabbdc3f7d0011223344"
    @NotBlank
    private String reconciliationRunDocsId;

    // Built from the bank's own values so the same line cannot load twice.
    // Example: "2026-04-08|5000.00|UPI/445512789/PRIYA"
    @NotBlank
    private String statementLineKey;

    // Date the bank applied the amount. Example: 2026-04-08
    @NotNull
    private LocalDate valueDate;

    // Text exactly as the bank sent it.
    // Example: "UPI/445512789/PRIYA SHARMA/SBIN"
    private String description;

    // Amount on the statement line, always positive. Example: 5000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Whether the bank put money in or took it out.
    // Example: LedgerEntryDirection.CREDIT
    @NotNull
    private LedgerEntryDirection direction;

    // Reference the bank gave the line. Example: "445512789"
    private String bankReference;

    // Example: ReconciliationMatchState.MATCHED
    @NotNull
    @Builder.Default
    private ReconciliationMatchState matchState = ReconciliationMatchState.UNMATCHED;

    // Links to FeePayment.id this line was matched to.
    // Example: "67ae1122dc3f7d0011223344"
    private String feePaymentDocsId;

    // Links to JournalEntry.id this line was matched to.
    // Example: "67ae5566dc3f7d0055667788"
    private String journalEntryDocsId;

    // Links to SettlementBatch.id when the line is a gateway payout.
    // Example: "67ad99aadc3f7d0099001122"
    private String settlementBatchDocsId;

    // Links to the staff identity that confirmed the match.
    // Example: "67aa15d9dc3f7d0055555555"
    private String matchedByDocsId;

    // Which rule found the suggestion. Example: "UPI_REFERENCE_EXACT"
    private String matchingRuleKey;

    // How sure the automatic match was, from 0 to 1. Example: 0.94
    private Double confidenceScore;

    // Why the line was left out or raised with the bank.
    // Example: "Bank charge, posted separately as an expense."
    private String remarks;
}
