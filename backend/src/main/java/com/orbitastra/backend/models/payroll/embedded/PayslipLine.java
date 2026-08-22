package com.orbitastra.backend.models.payroll.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.payroll.enums.SalaryComponentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line printed on one payslip.
 *
 * <p>It has no collection of its own. A payslip's lines are always read together with it.
 *
 * <p>The name and type are **copied in**, not read through the component link. A payslip is
 * a statement of what somebody was paid, and reprinting it three years later must produce
 * the same paper even after the school has renamed House Rent Allowance or stopped using it
 * altogether. Same rule FeeInvoiceLine follows for the fee head name.
 *
 * <p>{@code ratePercent} is kept where the amount came from a percentage, because a member
 * of staff asking "why is my house rent allowance this figure" deserves to see the working
 * rather than only the answer.
 *
 * <p>{@code adHoc} marks a line that was not on the salary structure: a bonus, an overtime
 * payment, a deduction for unpaid leave, recovery of an advance. Those are real every month
 * and cannot live on the structure, which is standing. Marking them means a payslip can be
 * read against the structure and the differences explained, instead of the two quietly
 * disagreeing.
 *
 * <p>An ad-hoc line always carries a {@code reason}. A deduction somebody cannot explain is
 * the one thing guaranteed to reach the head's office.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipLine {

    // Links to SalaryComponent.id. Null for an ad-hoc line with no component behind it.
    // Example: "67bd1122dc3f7d0011223344"
    private String salaryComponentDocsId;

    // Name as printed, copied in so a reprint years later matches.
    // Example: "House Rent Allowance"
    @NotBlank
    private String componentName;

    // Copied in for the same reason. Example: SalaryComponentType.EARNING
    @NotNull
    private SalaryComponentType componentType;

    // The figure on this line. Always positive; the type says which way it moves.
    // Example: 19200.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // The share it was worked out from, where it came from a percentage. Kept so the
    // working can be shown. Example: 40.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal ratePercent;

    // True for a line that was not on the salary structure: a bonus, overtime, unpaid
    // leave, recovery of an advance. Example: false
    @NotNull
    @Builder.Default
    private Boolean adHoc = false;

    // Why this line is here. Required for an ad-hoc line.
    // Example: "Three days unpaid leave in August."
    private String reason;

    // Order the line appears in. Example: 20
    private Integer sortOrder;
}
