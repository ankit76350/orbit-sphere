package com.orbitastra.backend.models.payroll;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.payroll.enums.PayrollRunStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One month's payroll for the whole school.
 *
 * <p>One row per month. It exists so a month is a thing that can be worked out, checked,
 * approved and closed as a unit, rather than payslips appearing one at a time with nobody
 * able to say whether the month is finished.
 *
 * <p>COMPUTED and APPROVED are separate on purpose. Computing works out what everybody
 * should get and is safe to run again as often as you like, which matters because somebody
 * always finds a missing allowance on the first pass. Approving is a person agreeing to the
 * figures, and after that they must not move. One status covering both would mean every
 * recalculation silently re-approved itself.
 *
 * <p>Once PAID a run is closed for good. A mistake found afterwards is corrected in the
 * next month's run, not by reopening a month people have already been paid for and whose
 * figures a bank statement now shows.
 *
 * <p>{@code payPeriodKey} is stored as a plain sortable string rather than a year and a
 * month in two fields, so a list of runs sorts and a lookup is one match instead of two.
 *
 * <p>The totals are running sums of the payslips and must always be rebuildable from them,
 * the same rule FeeInvoice follows for its payment totals. They are here so a management
 * report loads without adding up two hundred payslips.
 *
 * <p>{@code totalCostToSchool} is not the same number as {@code totalGrossAmount}, and both
 * are asked for. Gross is what staff earn; cost adds the employer's own contributions,
 * which nobody takes home but the school still spends.
 *
 * <p>The service checks that only one run exists per month, that nothing is paid before
 * APPROVED, that a PAID run is never reopened, that the approver is not the person who
 * computed it, and that every member of staff with a current salary structure and active
 * employment gets a payslip.
 */
@Document(collection = "payroll_runs")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_payroll_run_period_uniq",
                def = "{'schoolId': 1, 'payPeriodKey': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_payroll_run_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'payPeriodKey': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRun extends SchoolBase {

    // The month, as a string that sorts. Example: "2026-08"
    @NotBlank
    private String payPeriodKey;

    // First day of the month being paid for. Example: 2026-08-01
    @NotNull
    private LocalDate periodStart;

    // Last day of it. Example: 2026-08-31
    @NotNull
    private LocalDate periodEnd;

    // Example: PayrollRunStatus.PAID
    @NotNull
    @Builder.Default
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    // How many payslips this run produced. Example: 214
    @NotNull
    @Builder.Default
    private Integer payslipCount = 0;

    // Total earnings across every payslip. Example: 9840000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalGrossAmount = BigDecimal.ZERO;

    // Total deductions across every payslip. Example: 1284000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalDeductionAmount = BigDecimal.ZERO;

    // What actually goes out to banks. Example: 8556000.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalNetAmount = BigDecimal.ZERO;

    // What the month costs the school, including its own contributions. Bigger than
    // gross, and a different question. Example: 10430400.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalCostToSchool = BigDecimal.ZERO;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Links to Staff.id for whoever ran the computation.
    // Example: "67aa15d9dc3f7d0044444444"
    private String computedByStaffDocsId;

    // The last time it was computed. Recomputing is safe and expected.
    // Example: 2026-08-26T05:30:00Z
    private Instant computedAt;

    // Links to Staff.id for whoever agreed the figures. Never the same person who
    // computed them. Example: "67aa15d9dc3f7d0055555555"
    private String approvedByStaffDocsId;

    // Example: 2026-08-27T06:00:00Z
    private Instant approvedAt;

    // When the money went out. Example: 2026-08-28T04:00:00Z
    private Instant paidAt;

    // Links to BankAccount.id the salaries were paid from.
    // Example: "67ad8899dc3f7d0088990011"
    private String bankAccountDocsId;

    // Why it was cancelled. Needed when the status is CANCELLED.
    // Example: "Computed before the August increments were entered."
    private String cancellationReason;

    // Example: "Two payslips withheld pending bank details."
    private String remarks;
}
