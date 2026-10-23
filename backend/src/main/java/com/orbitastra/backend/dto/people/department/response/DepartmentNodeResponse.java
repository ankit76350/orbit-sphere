package com.orbitastra.backend.dto.people.department.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.department.Department;

/**
 * One unit in the tree, with whatever hangs off it.
 *
 * <p><b>The same fields as {@link DepartmentResponse}, plus {@code subDepartments}.</b> Two records
 * rather than one with a sometimes-populated list: a field present on some responses and absent on
 * others is a field every client has to guard, which is the call this project already made for
 * #28 and #29 of academics.
 *
 * <p><b>{@code subDepartments} is always an array, never null</b>, so a client can recurse into
 * a leaf without checking first.
 */
public record DepartmentNodeResponse(
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

        /**
         * True when this unit's parent is not in the answer, so it was lifted to the top.
         *
         * <p>Absent on an ordinary root. It appears when {@code ?active=true} excluded a retired
         * parent whose children are still active — those children are real and must not vanish,
         * so they surface here rather than being dropped with the parent.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean liftedToTop,

        List<DepartmentNodeResponse> subDepartments) {

    public static DepartmentNodeResponse of(Department department, boolean lifted,
            List<DepartmentNodeResponse> subDepartments) {

        return new DepartmentNodeResponse(
                department.getId(),
                department.getDepartmentCode(),
                department.getName(),
                department.getDescription(),
                department.getParentDepartmentDocsId(),
                department.getHeadStaffDocsId(),
                department.getActive(),
                lifted ? Boolean.TRUE : null,
                subDepartments);
    }
}
