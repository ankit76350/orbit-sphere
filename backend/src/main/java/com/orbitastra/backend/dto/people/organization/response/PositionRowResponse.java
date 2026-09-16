package com.orbitastra.backend.dto.people.organization.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.organization.Position;

/**
 * One seat on #15's page, with the count of who currently holds it.
 *
 * <h2>Why this is not {@link PositionResponse}</h2>
 *
 * <p>Because that record says, in its own javadoc, that the filled count is not on it and never
 * will be: it is what the two writes answer with, and a number derived from another collection
 * has no business on the response to a write that did not read that collection. It would be stale
 * the moment somebody is hired.
 *
 * <p>This record is what a <b>read</b> answers with, and a read can afford the second query.
 *
 * <h2>The count is computed, never stored</h2>
 *
 * <p>{@code filledHeadcount} is counted from current employment records every time. A stored
 * counter on {@code Position} drifts the first time a writer forgets it — the objection that also
 * keeps a weight total off {@code AcademicTerm} and a gap warning off {@code GradingScheme}.
 *
 * <h2>{@code vacancies} never goes below zero, and {@code overFilled} is how you tell</h2>
 *
 * <p>A school may hire a twelfth teacher into eleven approved seats — #16 <b>warns</b> rather than
 * refusing, because that is a budget conversation and not a data error. So {@code filled} can
 * exceed {@code approved}, and a negative {@code vacancies} would read as a seat owing people.
 * The overflow is reported as a flag instead, which is also what #14's {@code HEADCOUNT_BELOW_FILLED}
 * is about.
 */
public record PositionRowResponse(
        String positionDocsId,
        String title,
        String departmentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reportsToPositionDocsId,

        Integer approvedHeadcount,

        /** How many people hold it right now. Counted from employment records, never stored. */
        long filledHeadcount,

        /** Approved minus filled, floored at zero. */
        long vacancies,

        /** Whether more people hold it than were approved. See the class note. */
        boolean overFilled,

        Boolean teachingPosition,
        Boolean active) {

    public static PositionRowResponse of(Position position, long filled) {

        int approved = position.getApprovedHeadcount() == null
                ? 0
                : position.getApprovedHeadcount();

        return new PositionRowResponse(
                position.getId(),
                position.getTitle(),
                position.getDepartmentDocsId(),
                position.getReportsToPositionDocsId(),
                position.getApprovedHeadcount(),
                filled,
                Math.max(0L, approved - filled),
                filled > approved,
                position.getTeachingPosition(),
                position.getActive());
    }
}
