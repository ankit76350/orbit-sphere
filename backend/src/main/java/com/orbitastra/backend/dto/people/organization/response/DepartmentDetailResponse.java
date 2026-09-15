package com.orbitastra.backend.dto.people.organization.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One department and everything it is made of. Endpoint #52.
 *
 * <p><b>This is the one endpoint in the package that resolves an id to a name.</b> Everywhere else
 * {@code parentDepartmentDocsId} and {@code headStaffDocsId} come back raw, because one place
 * should decide how a unit and a person are presented and a write's response is not it. A detail
 * view <i>is</i> that place: the whole question it answers is "tell me about this unit", and
 * making a caller issue three more requests to render one page is the cost of refusing.
 *
 * <p><b>The positions are here, and that is a boundary worth stating.</b> This answers "what is
 * this unit made of". #15 — {@code GET /positions}, not built — answers "find seats across the
 * school", filtered and paged. When it arrives the two will return the same rows in two shapes,
 * which is the situation #29 of academics ended up in and had to be trimmed out of. **If it
 * becomes a problem, #15 wins and this trims**, for the same reason: the endpoint that owns the
 * question keeps it.
 *
 * <p><b>Direct children only.</b> The whole nesting is #12 with {@code ?tree=true}, which builds
 * it from one flat read; repeating that here would be a second implementation of the same walk.
 *
 * <p><b>Not paged.</b> This is one document's composition, not a list — a department has a handful
 * of seats and a handful of children, and a page cursor over either would be machinery nobody uses.
 */
public record DepartmentDetailResponse(
        String departmentDocsId,
        String departmentCode,
        String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,

        Boolean active,

        /** The unit this one sits under, resolved. Absent for a top-level department. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        DepartmentSummaryResponse parent,

        /** Who runs it, resolved to a name and nothing more. Absent when none is named. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        StaffSummaryResponse headStaff,

        /** The units directly under this one, by name. Empty for a leaf, never null. */
        List<DepartmentSummaryResponse> children,

        /** Every seat in this unit, by title, retired ones included and marked. */
        List<PositionResponse> positions,

        int childCount,
        int positionCount,
        int activePositionCount,

        /**
         * How many seats here are marked {@code teachingPosition}.
         *
         * <p>Zero is legitimate for Finance — and it is also exactly what an empty teacher picker
         * looks like, which is why #13 warns about it on the way in and this counts it on the way
         * out.
         */
        int teachingPositionCount) {
}
