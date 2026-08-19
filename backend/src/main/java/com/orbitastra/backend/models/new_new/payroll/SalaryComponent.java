package com.orbitastra.backend.models.new_new.payroll;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.payroll.enums.ComponentCalculation;
import com.orbitastra.backend.models.new_new.payroll.enums.SalaryComponentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One line that can appear on a payslip.
 *
 * <p>Basic Pay, House Rent Allowance, Transport Allowance, Warden Allowance, Provident
 * Fund, Professional Tax, Income Tax. The school sets these up once and builds everybody's
 * salary out of them.
 *
 * <p>**This is the decision the whole package turns on.** The reference sketch held
 * {@code basicSalary}, {@code hra}, {@code da}, {@code pf}, {@code esi},
 * {@code professionalTax} and {@code tds} as fixed columns on the salary structure and
 * again on the payslip. A school that pays a Warden Allowance, a Hostel Duty Allowance or a
 * Bus Escort Allowance then has nowhere to put it, and adding one means changing the
 * database. Every school has at least one allowance nobody else has.
 *
 * <p>So components are rows a school creates, and a salary is a list of them.
 *
 * <p>{@code calculation} matters because most components in an Indian school are a share of
 * basic rather than a figure. House rent allowance is normally 40 or 50 percent of basic, so
 * raising basic pay should carry it along instead of somebody editing two numbers for every
 * member of staff.
 *
 * <p>{@code statutory} marks the ones the law requires: provident fund, employees' state
 * insurance, professional tax, income tax. The flag exists so a payslip can group them and
 * so an accountant can be given the list, **not** because this system computes them
 * properly. It does not: rates, wage ceilings and state slabs change with legislation, and
 * getting a provident fund ceiling wrong is a compliance failure rather than a bug. The
 * amounts come from the school or its accountant. See the README.
 *
 * <p>The service checks that a component used by any salary structure is never deleted, and
 * that a percentage component carries a rate while a fixed one carries an amount.
 */
@Document(collection = "salary_components")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_salary_component_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_salary_component_order_idx",
                def = "{'schoolId': 1, 'componentType': 1, 'active': 1, 'sortOrder': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryComponent extends SchoolBase {

    // Name as it is printed on the payslip. Example: "House Rent Allowance"
    @NotBlank
    private String name;

    // Short form for a narrow payslip column. Example: "HRA"
    private String shortName;

    // Whether it adds to pay, is taken off it, or is paid by the school on top.
    // Example: SalaryComponentType.EARNING
    @NotNull
    private SalaryComponentType componentType;

    // How the amount is worked out.
    // Example: ComponentCalculation.PERCENT_OF_BASIC
    @NotNull
    private ComponentCalculation calculation;

    // The share, when the calculation is a percentage. Example: 40.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal defaultRatePercent;

    // The figure, when the calculation is a fixed amount. A starting value only; each
    // salary structure sets its own. Example: 1600.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal defaultAmount;

    // Whether income tax applies to this earning. Example: true
    @NotNull
    @Builder.Default
    private Boolean taxable = true;

    // True for the ones the law requires, so a payslip can group them and an accountant
    // can be handed the list. This system does not compute them. Example: false
    @NotNull
    @Builder.Default
    private Boolean statutory = false;

    // Whether this is the one component every percentage is worked out from. Exactly one
    // component per school has this set, and it is normally Basic Pay. Example: false
    @NotNull
    @Builder.Default
    private Boolean isBasicPay = false;

    // Order the line appears in on the payslip. Example: 10
    @Builder.Default
    private Integer sortOrder = 0;

    // Example: "Forty percent of basic for staff in city accommodation."
    private String description;

    // Whether new salary structures may still use this. Turning it off leaves the
    // structures already using it alone. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
