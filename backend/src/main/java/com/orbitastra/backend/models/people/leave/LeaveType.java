package com.orbitastra.backend.models.people.leave;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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
 * School-configurable leave policy category such as casual, sick, earned, or
 * unpaid leave.
 */
@Document(collection = "staff_leave_types")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_leave_type_code_uniq",
                def = "{'schoolId': 1, 'leaveTypeCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_leave_type_active_name_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType extends SchoolBase {

    // Stable school-scoped business key. Example: "SICK_LEAVE"
    @NotBlank
    private String leaveTypeCode;

    // Example: "Sick Leave"
    @NotBlank
    private String name;

    // Example: "Leave provided for illness or medical recovery."
    private String description;

    // Default allowance for one academic year. Example: 12.0
    // annualAllowanceDays : means how many leave days a staff member receives each academic year.
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal annualAllowanceDays = BigDecimal.ZERO;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean paid = true;

    // carryForwardAllowed : determines whether unused leave can move to the next academic year.
    // Example: false
    @NotNull
    @Builder.Default
    private Boolean carryForwardAllowed = false;

    // Maximum days that may carry into the next year. Example: 5.0
    // maximumCarryForwardDays : limits how many unused days can move forward.
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    @Builder.Default
    private BigDecimal maximumCarryForwardDays = BigDecimal.ZERO;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
