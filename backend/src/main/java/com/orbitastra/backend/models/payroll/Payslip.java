package com.orbitastra.backend.models.new_new.payroll;

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
import com.orbitastra.backend.models.new_new.common.enums.PaymentMode;
import com.orbitastra.backend.models.new_new.payroll.embedded.PayslipLine;
import com.orbitastra.backend.models.new_new.payroll.enums.PayslipStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * What one member of staff was paid for one month.
 *
 * <p>Once the run it belongs to is APPROVED, a payslip is a statement the school has given
 * somebody about their own money. It is never edited and never deleted after that. A
 * mistake is put right in the next month's run, because a bank statement now shows what
 * actually went out and rewriting the paper would not change that.
 *
 * <p>Everything is snapshotted onto the lines rather than read back through the salary
 * structure: the component names, their types, the rates. A payslip printed again in 2030
 * has to come out exactly as it did, even after the school has renamed an allowance or
 * stopped paying it. Same rule FeeInvoiceLine and IssuedDocument follow.
 *
 * <p>{@code salaryStructureDocsId} is kept so somebody can see which structure the figures
 * came from, but the payslip does not depend on it still existing in that form.
 *
 * <p>{@code paidDays} and {@code unpaidLeaveDays} are why a month's pay can differ from the
 * structure. Unpaid leave comes from the staff leave records, and the resulting reduction
 * appears as an ad-hoc deduction line so the staff member can see what was taken and why.
 *
 * <p>WITHHELD lets one person's pay be held while everybody else is paid: a bank account
 * that will not verify, an unsettled advance, somebody who left mid-month. Without it,
 * holding one person back would mean holding the whole school.
 *
 * <p>{@code documentRecordDocsId} is the printed payslip itself. Handing it out is what
 * makes this a statement rather than a calculation, and the stored file is what a reprint
 * gives back rather than the figures being worked out afresh.
 *
 * <p>Nothing here says a payslip was emailed. That is the notification system, which is
 * designed last.
 *
 * <p>The service checks that the lines add up to the totals, that a payslip is never edited
 * once its run is APPROVED, that one payslip exists per member of staff per run, that an
 * ad-hoc line carries a reason, and that reading a payslip needs the PAYROLL module or is
 * the staff member's own.
 */
@Document(collection = "payslips")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_payslip_no_uniq",
                def = "{'schoolId': 1, 'payslipNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_payslip_run_staff_uniq",
                def = "{'schoolId': 1, 'payrollRunDocsId': 1, 'staffDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_payslip_staff_history_idx",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'payPeriodKey': -1}"),
        @CompoundIndex(
                name = "school_payslip_status_idx",
                def = "{'schoolId': 1, 'payPeriodKey': -1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends SchoolBase {

    // School-scoped number from NumberSequence type PAYSLIP, quoted when a staff member
    // asks about a month. Example: "PS/2026/08/00214"
    @NotBlank
    private String payslipNo;

    // Links to PayrollRun.id. Example: "67bd1125dc3f7d0044556677"
    @NotBlank
    private String payrollRunDocsId;

    // Links to Staff.id. Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String staffDocsId;

    // The month, copied from the run so a person's payslip history reads without loading
    // every run. Example: "2026-08"
    @NotBlank
    private String payPeriodKey;

    // Links to SalaryStructure.id the figures came from, for anybody who wants to see
    // the working. The payslip does not depend on it. Example: "67bd1126dc3f7d0055667788"
    private String salaryStructureDocsId;

    // Every line printed on the slip, with names and rates copied in.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<PayslipLine> lines = new ArrayList<>();

    // Total of the earning lines. Example: 48000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal grossAmount;

    // Total of the deduction lines. Example: 6200.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    // What reached the bank: gross minus deductions. Example: 41800.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal netAmount;

    // The school's own contributions for this person this month. Never taken off their
    // pay, and never part of net. Example: 2880.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal employerContributionAmount = BigDecimal.ZERO;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // How many days of the month were paid. Example: 28
    private Integer paidDays;

    // Days of unpaid leave, which is why a month can differ from the structure. The
    // reduction itself appears as an ad-hoc deduction line. Example: 3
    @NotNull
    @Builder.Default
    private Integer unpaidLeaveDays = 0;

    // Example: PayslipStatus.PAID
    @NotNull
    @Builder.Default
    private PayslipStatus status = PayslipStatus.DRAFT;

    // How the money was handed over. Not every school pays every member of staff by
    // transfer: support staff are often paid in cash, and a system that assumed a transfer
    // would have nowhere to record that, which means no record of the payment at all.
    // Example: PaymentMode.BANK_TRANSFER
    private PaymentMode paymentMode;

    // When the money went out for this person. Example: 2026-08-28T04:00:00Z
    private Instant paidAt;

    // The bank's own reference for the transfer, not the school's. Its one job is that a
    // staff member says they were never paid and somebody can quote this to the bank.
    // Example: "NEFT/SAL/202608/00214"
    private String paymentReference;

    // Links to StaffBankAccount.id the salary was sent to.
    // Example: "67be1123dc3f7d0022334455"
    private String staffBankAccountDocsId;

    // The masked account number as it stood when this month was paid, copied in. A staff
    // member who changes banks in November must not make August's payslip claim the new
    // account. Example: "XXXXXX4821"
    private String paidToAccountMasked;

    // The bank and IFSC used, copied in for the same reason.
    // Example: "State Bank of India"
    private String paidToBankName;

    // Links to DocumentRecord.id for the printed payslip. A reprint hands out this file
    // rather than working the figures out again. Example: "67bd1127dc3f7d0066778899"
    private String documentRecordDocsId;

    // Why this one was held back or cancelled. Needed for WITHHELD and CANCELLED.
    // Example: "Bank account details did not verify; asked for a fresh passbook copy."
    private String statusReason;

    // Example: "Includes the arrear from the April increment."
    private String remarks;
}
