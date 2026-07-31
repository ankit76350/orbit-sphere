package com.orbitastra.backend.models.new_new.people.organization;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.employment.enums.EmploymentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * School-approved job position that can be assigned through an EmploymentRecord.
 *
 * <p>{@code departmentDocsId} links to Department.id. Position hierarchy uses
 * {@code reportsToPositionDocsId}; an individual manager override can still be
 * stored in EmploymentRecord.managerDocsId. Filled headcount is calculated from
 * current employment records and is not duplicated here.
 */
@Document(collection = "staff_positions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_position_code_uniq",
                def = "{'schoolId': 1, 'positionCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_department_position_active_idx",
                def = "{'schoolId': 1, 'departmentDocsId': 1, 'active': 1, 'title': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Position extends SchoolBase {

    // Stable school-scoped business key. Example: "MATH_TEACHER"
    @NotBlank
    private String positionCode;

    // Example: "Mathematics Teacher"
    @NotBlank
    private String title;

    // Links to Department.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String departmentDocsId;

    // Optionally links to the supervising Position.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String reportsToPositionDocsId;

    // Example: EmploymentType.FULL_TIME
    @NotNull
    private EmploymentType employmentType;

    // Approved number of employees for this position. Example: 8
    @NotNull
    @Builder.Default
    private Integer approvedHeadcount = 1;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean teachingPosition = false;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
