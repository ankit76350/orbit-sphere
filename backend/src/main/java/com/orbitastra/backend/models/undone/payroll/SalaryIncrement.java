package com.orbitastra.backend.models.undone.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryIncrement {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

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
