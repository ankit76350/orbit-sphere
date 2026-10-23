package com.orbitastra.backend.models.people.leave;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Leave balance aggregate for one Staff member, LeaveType, and academic year.
 *
 * <p>Available balance is calculated from the stored components rather than
 * persisted as another field:
 *
 * <pre>
 * allocatedDays + carriedForwardDays + adjustmentDays - usedDays - pendingDays
 * </pre>
 *
 * <p>When a request is submitted, its days are added to {@code pendingDays}.
 * When approved, the same days are moved from {@code pendingDays} to
 * {@code usedDays}. A request must never be counted in both fields.
 *
 * <p>The inherited optimistic-lock version protects concurrent approvals from
 * silently overwriting balance changes.
 */
@Document(collection = "staff_leave_balances")
@CompoundIndex(
        name = "school_year_staff_leave_type_uniq",
        def = "{'schoolId': 1, 'academicYear': 1, 'staffDocsId': 1, 'leaveTypeDocsId': 1}",
        unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeaveBalance extends SchoolBase {

    // Links to Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Links to LeaveType.id.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String leaveTypeDocsId;

    // Stores AcademicYear.name, never its document id. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Allowance assigned for the year. Example: 12.0
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal allocatedDays = BigDecimal.ZERO;

    // Days brought from the previous year. Example: 3.0
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal carriedForwardDays = BigDecimal.ZERO;

    // Total approved leave, including approved leave scheduled in the future.
    // Example: 4.5
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    // Submitted leave still awaiting approval; approved leave is excluded.
    // Example: 2.0
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal pendingDays = BigDecimal.ZERO;

    // Net manual correction: a positive value adds leave; a negative value removes it.
    // Example: 1.0 or -1.0
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal adjustmentDays = BigDecimal.ZERO;
}
