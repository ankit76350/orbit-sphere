package com.orbitastra.backend.models.new_new.finance.accounting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.accounting.embedded.BudgetLine;
import com.orbitastra.backend.models.new_new.finance.enums.ApprovalStatus;

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
 * What the school plans to earn and spend in one financial year, account by
 * account.
 *
 * <p>Budgets are versioned rather than edited, in the same way fee structures
 * are. A revision during the year becomes {@code budgetVersion + 1}, so a
 * comparison run in March still shows the numbers the school was working to at
 * the time.
 *
 * <p>Only an APPROVED version should be used for checking spend. A draft is a
 * working document, and the service must not warn about overspending against
 * numbers nobody has agreed to yet.
 */
@Document(collection = "budget_plans")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_fiscal_budget_version_uniq",
                def = "{'schoolId': 1, 'fiscalYearKey': 1, 'budgetCode': 1, 'budgetVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_fiscal_budget_status_idx",
                def = "{'schoolId': 1, 'fiscalYearKey': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPlan extends SchoolBase {

    // Financial year this budget covers. Example: "2026-2027"
    @NotBlank
    private String fiscalYearKey;

    // Stable key for the budget across its versions. Example: "ANNUAL_OPERATING"
    @NotBlank
    private String budgetCode;

    // Version of the budget, starting at 1. Example: 1
    @NotNull
    @Builder.Default
    private Integer budgetVersion = 1;

    // Example: "Annual Operating Budget 2026-2027"
    @NotBlank
    private String name;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: ApprovalStatus.APPROVED
    @NotNull
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    // Planned income across every line. Example: 42000000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalPlannedIncome;

    // Planned spend across every line. Example: 38500000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalPlannedExpense;

    // The account and period amounts.
    @Valid
    @Builder.Default
    private List<BudgetLine> lines = new ArrayList<>();

    // Links to the staff identity that prepared the budget.
    // Example: "67aa15d9dc3f7d0044444444"
    private String preparedByDocsId;

    // Links to the staff identity that approved it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Example: 2026-03-25T10:00:00Z
    private Instant approvedAt;

    // Version that replaced this one. Example: "67b01122dc3f7d0011223344"
    private String supersededByBudgetDocsId;

    // Why the budget was revised.
    // Example: "Revised after two extra sections were opened in class I."
    private String revisionReason;
}
