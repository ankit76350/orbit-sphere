package com.orbitastra.backend.models.people.organization;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

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
 * School-defined organizational department such as Academics, Finance, HR, or
 * Transport.
 *
 * <p>Positions link to this document through {@code departmentDocsId}. A
 * department may optionally link to a parent department and a Staff member who
 * acts as its head.
 */
@Document(collection = "staff_departments")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_department_code_uniq",
                def = "{'schoolId': 1, 'departmentCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_department_active_name_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends SchoolBase {

    // Stable school-scoped business key. Example: "ACADEMICS"
    @NotBlank
    private String departmentCode;

    // Example: "Academic Department"
    @NotBlank
    private String name;

    // Example: "Responsible for curriculum and teaching operations."
    private String description;

    // Optionally links to another Department.id.
    // Example: "67aa15d9dc3f7d0011111111"
    private String parentDepartmentDocsId;

    // Optionally links to the department head's Staff.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String headStaffDocsId;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
