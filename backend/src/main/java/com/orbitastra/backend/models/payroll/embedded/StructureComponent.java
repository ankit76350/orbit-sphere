package com.orbitastra.backend.models.payroll.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.payroll.enums.ComponentCalculation;
import com.orbitastra.backend.models.payroll.enums.SalaryComponentType;

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
 * <p>The type and the calculation are copied in; the **name is not**, and the difference
 * matters.
 *
 * <p>A structure is a live agreement about what somebody is paid now, so it should show the
 * component's current name. Freezing the name here would manufacture staleness rather than
 * prevent it: rename the component and the structure screen would keep showing the old
 * wording for no good reason. The name is read through {@code salaryComponentDocsId}.
 *
 * <p>A payslip is the opposite case. It is a statement about a month that has already
 * happened, so PayslipLine does snapshot the name — a payslip reprinted in 2030 has to come
 * out as it did at the time.
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

    // Copied in so the structure can check its own arithmetic. Rebuilding the stored
    // gross and deduction totals means knowing which lines add and which take away, and a
    // record that cannot verify itself without loading five other documents is weaker
    // than one that can. Example: SalaryComponentType.EARNING
    @NotNull
    private SalaryComponentType componentType;

    // Copied in because it says how to read the two fields below it. If the school later
    // switches this component from a percentage to a fixed amount, every structure holding
    // a rate of 40 would become unreadable without its own copy of what that 40 meant.
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
