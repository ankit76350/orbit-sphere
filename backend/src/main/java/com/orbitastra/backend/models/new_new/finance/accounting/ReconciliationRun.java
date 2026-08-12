package com.orbitastra.backend.models.new_new.finance.accounting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.ReconciliationRunStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One attempt at matching a bank statement against what the books say.
 *
 * <p>This is the check that catches money the school thinks it has but the bank
 * does not, and money in the bank nobody has accounted for. Each statement line
 * becomes a ReconciliationItem under this run.
 *
 * <p>A run is kept even after it finishes, and {@code runNo} allows more than one
 * attempt on the same statement period, so a second pass after a correction does
 * not wipe out the record of the first.
 *
 * <p>{@code unmatchedAmount} is the number that matters. A completed run with a
 * non-zero unmatched amount means the difference has been explained in the items,
 * not that it has gone away.
 */
@Document(collection = "reconciliation_runs")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_bank_statement_period_run_uniq",
                def = "{'schoolId': 1, 'bankAccountDocsId': 1, 'statementPeriodKey': 1, 'runNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_reconciliation_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'statementEndDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun extends SchoolBase {

    // Links to BankAccount.id being matched. Example: "67ad8899dc3f7d0088990011"
    @NotBlank
    private String bankAccountDocsId;

    // Statement period being matched. Example: "2026-04"
    @NotBlank
    private String statementPeriodKey;

    // Attempt number for this account and period, starting at 1. Example: 1
    @NotNull
    @Builder.Default
    private Integer runNo = 1;

    // Example: 2026-04-01
    private LocalDate statementStartDate;

    // Example: 2026-04-30
    private LocalDate statementEndDate;

    // Example: ReconciliationRunStatus.COMPLETED
    @NotNull
    @Builder.Default
    private ReconciliationRunStatus status = ReconciliationRunStatus.IN_PROGRESS;

    // Links to DocumentRecord.id for the statement file that was loaded.
    // Example: "67ad3344dc3f7d0044556677"
    private String statementDocumentDocsId;

    // Balance the statement opens with. Example: 1250000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal openingBalance;

    // Balance the statement closes with. Example: 1875400.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal closingBalance;

    // Balance the books show for the same date. Example: 1878900.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal ledgerClosingBalance;

    // Statement lines matched to the books. Example: 214
    @Builder.Default
    private Integer matchedCount = 0;

    // Statement lines still not matched. Example: 3
    @Builder.Default
    private Integer unmatchedCount = 0;

    // Money the statement and the books still disagree on. Example: 3500.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unmatchedAmount = BigDecimal.ZERO;

    // Example: 2026-05-02T04:00:00Z
    private Instant startedAt;

    // Example: 2026-05-02T09:40:00Z
    private Instant completedAt;

    // Links to the staff identity that signed the run off.
    // Example: "67aa15d9dc3f7d0055555555"
    private String completedByDocsId;

    // What is behind the remaining difference, or why the run was given up on.
    // Example: "Three cheques were still in clearing on 30 April."
    private String remarks;
}
