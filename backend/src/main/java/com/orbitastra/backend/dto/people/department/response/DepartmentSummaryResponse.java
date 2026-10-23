package com.orbitastra.backend.dto.people.department.response;

import com.orbitastra.backend.models.people.department.Department;

/**
 * Just enough of a department to name it on another department's page.
 *
 * <p><b>Four fields, deliberately.</b> This appears as the parent and as each child on #52, where
 * the caller wants to know <i>which</i> unit, not everything about it — and a full
 * {@link DepartmentResponse} nested twice over would make the page mostly repetition.
 *
 * <p>{@code active} is here because a retired parent is the interesting case: a live unit hanging
 * under a closed one is a state a school needs to see, and hiding the flag would hide it.
 */
public record DepartmentSummaryResponse(
        String departmentDocsId,
        String departmentCode,
        String name,
        Boolean active) {

    public static DepartmentSummaryResponse of(Department department) {
        return new DepartmentSummaryResponse(
                department.getId(),
                department.getDepartmentCode(),
                department.getName(),
                department.getActive());
    }
}
