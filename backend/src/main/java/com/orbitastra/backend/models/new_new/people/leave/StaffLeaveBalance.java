package com.orbitastra.backend.models.new_new.people.leave;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

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
 * allocated + carriedForward + adjustments - used - pending
 * </pre>
 *
 * <p>The inherited optimistic-lock version protects concurrent approvals from
 * silently overwriting balance changes.
 */
@Document(collection = "staff_leave_balances")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_staff_leave_type_year_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'leaveTypeDocsId': 1, 'academicYear': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_leave_balance_year_staff_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'staffDocsId': 1}")
})
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

    // Approved leave deducted from the balance, including future approved leave.
    // Example: 4.5
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    // Submitted/approved days reserved for future leave. Example: 2.0
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal pendingDays = BigDecimal.ZERO;

    // Manual credit or debit adjustment. Example: 1.0 or -1.0
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal adjustmentDays = BigDecimal.ZERO;
}
