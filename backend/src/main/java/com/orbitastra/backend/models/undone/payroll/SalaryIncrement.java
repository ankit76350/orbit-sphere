package com.orbitastra.backend.models.undone.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.payroll.enums.IncrementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An appraisal-driven salary increment for a staff member. Once approved and
 * effective, it updates the staff member's {@link SalaryStructure}.
 */
@Document(collection = "salary_increments")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryIncrement extends SchoolBase {

    @Indexed
    private String staffId;

    // Increment percentage applied to the previous salary.
    private BigDecimal percent;

    private BigDecimal newSalary;

    @Builder.Default
    private IncrementStatus status = IncrementStatus.DRAFT;

    private LocalDate effectiveDate;

    private String appliedBy; // references Staff.id / User.id
}
