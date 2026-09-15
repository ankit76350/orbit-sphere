package com.orbitastra.backend.dto.people.organization.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.organization.Position;

/**
 * One approved seat, as every position endpoint returns it.
 *
 * <p><b>{@code positionDocsId} is the whole identity.</b> {@code positionCode} was removed on
 * 2026-09-15, so there is no second way to name a seat — and {@code EmploymentRecord} already
 * stored the id rather than a code.
 *
 * <p><b>The filled count is not here, and never will be on this response.</b> It is derived from
 * current employment records, and storing or returning it from a write would be a number that is
 * stale the moment somebody is hired. #15 computes it.
 */
public record PositionResponse(
        String positionDocsId,
        String title,
        String departmentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reportsToPositionDocsId,

        Integer approvedHeadcount,
        Boolean teachingPosition,
        Boolean active,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static PositionResponse fromPosition(Position position) {
        return fromPosition(position, null, null);
    }

    public static PositionResponse fromPosition(Position position, String warning,
            String nextStep) {

        return new PositionResponse(
                position.getId(),
                position.getTitle(),
                position.getDepartmentDocsId(),
                position.getReportsToPositionDocsId(),
                position.getApprovedHeadcount(),
                position.getTeachingPosition(),
                position.getActive(),
                warning,
                nextStep);
    }
}
