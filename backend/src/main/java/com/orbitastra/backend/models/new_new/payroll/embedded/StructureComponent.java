package com.orbitastra.backend.models.new_new.payroll.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.payroll.enums.ComponentCalculation;
import com.orbitastra.backend.models.new_new.payroll.enums.SalaryComponentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One component in one person's salary, with the figure agreed for them.
 *
 * <p>It has no collection of its own. A structure's components are always read together,
 * and there are never many.
 *
 * <p>The component's name, type and calculation are **copied in** rather than read through
 * the link. That is on purpose: a school that reworks its House Rent Allowance next year
 * must not change what an existing structure means, and a payslip printed from it three
 * years later has to still read the way it did.
 *
 * <p>{@code amount} is used when the calculation is a fixed figure, and
 * {@code ratePercent} when it is a share. Only one of the two is ever set, and the service
 * checks it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureComponent {

    // Links to SalaryComponent.id. Example: "67bd1122dc3f7d0011223344"
    @NotBlank
    private String salaryComponentDocsId;

    // Name copied in when the structure was made, so a reprint years later matches.
    // Example: "House Rent Allowance"
    @NotBlank
    private String componentName;

    // Copied in for the same reason. Example: SalaryComponentType.EARNING
    @NotNull
    private SalaryComponentType componentType;

    // Copied in for the same reason.
    // Example: ComponentCalculation.PERCENT_OF_BASIC
    @NotNull
    private ComponentCalculation calculation;

    // The figure agreed for this person, when the calculation is a fixed amount.
    // Example: 2000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // The share agreed for this person, when the calculation is a percentage.
    // Example: 40.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal ratePercent;

    // Order the line appears in on the payslip, copied from the component.
    // Example: 20
    private Integer sortOrder;
}
