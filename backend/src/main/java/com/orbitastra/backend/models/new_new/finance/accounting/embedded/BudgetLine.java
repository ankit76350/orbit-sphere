package com.orbitastra.backend.models.new_new.finance.accounting.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How much one account is allowed in one period of a BudgetPlan.
 *
 * <p>It has no collection identity of its own. A budget is read as a whole when
 * comparing plan against actual, so the lines live inside the plan.
 *
 * <p>{@code periodNo} matches FiscalPeriod.periodNo, which is what lets a spend
 * report line up month by month against what was planned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLine {

    // Links to LedgerAccount.id. Example: "67ac20a1dc3f7d0077665544"
    @NotBlank
    private String ledgerAccountDocsId;

    // Account code copied in so the line reads on its own. Example: "5200"
    @NotBlank
    private String ledgerAccountCode;

    // Period inside the financial year this amount is for. Example: 1
    @NotNull
    private Integer periodNo;

    // Money allowed for this account in this period. Example: 150000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Cost centre the amount belongs to. Example: "PRIMARY_WING"
    private String costCentreCode;

    // Note about how the number was arrived at.
    // Example: "Based on last year's spend plus 8 percent."
    private String remarks;
}
