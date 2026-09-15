package com.orbitastra.backend.dto.people.organization.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.organization.Department;

/**
 * One org unit, as every department endpoint returns it.
 *
 * <p><b>{@code departmentDocsId} is what positions reference.</b> A position stores it and cannot
 * move between units afterwards, which is why a department is a document with an id rather than a
 * code embedded somewhere.
 *
 * <p><b>Nothing is resolved to a name.</b> {@code parentDepartmentDocsId} and
 * {@code headStaffDocsId} come back as raw ids. Resolving them is possible and deliberately not
 * done: one place should decide how a parent unit or a head is presented, and #12 — the tree read
 * — is where that decision belongs, not on every write's response.
 */
public record DepartmentResponse(
        String departmentDocsId,
        String departmentCode,
        String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String parentDepartmentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String headStaffDocsId,

        Boolean active,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static DepartmentResponse fromDepartment(Department department) {
        return fromDepartment(department, null);
    }

    public static DepartmentResponse fromDepartment(Department department, String nextStep) {
        return new DepartmentResponse(
                department.getId(),
                department.getDepartmentCode(),
                department.getName(),
                department.getDescription(),
                department.getParentDepartmentDocsId(),
                department.getHeadStaffDocsId(),
                department.getActive(),
                nextStep);
    }
}
