package com.orbitastra.backend.dto.people.department.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One seat and who is in it. Endpoint #53.
 *
 * <h2>Added after the plan, the way #52 was</h2>
 *
 * <p>The plan numbered #15 and stopped: a page of seats with their filled counts. But a count is
 * the beginning of a question, not the end of one — a school reading "3 of 5 filled" immediately
 * asks <i>which three</i>, and answering that from #15 plus #7 means knowing that #7 has no
 * employment filter yet. So the same call #52 made for a department is made here for a seat.
 *
 * <h2>The count and the list are computed from the same read</h2>
 *
 * <p>{@code filledHeadcount} is {@code holders.size()}, not a separate count query. Two reads of
 * the same collection a moment apart can disagree — somebody is hired between them — and a page
 * showing "3 filled" above two names is a bug report nobody can reproduce. #15 counts without
 * listing because a page of twenty seats should not fetch every holder of every one; this endpoint
 * lists, so it counts by listing.
 *
 * <h2>Current holders only</h2>
 *
 * <p>Not the seat's history. A closed record means somebody who <i>used to</i> hold this, and
 * "who is in this seat" is the question — the same reason {@code current} exists on the record at
 * all. One person's history is #19, and it is addressed by the person because that is who a
 * history belongs to.
 *
 * <p><b>A retired seat can still have holders</b>, and they are returned. #14 does not check the
 * filled count before retiring — that is the {@code POSITION_STILL_FILLED} it owes — so a seat
 * marked {@code active: false} with two people in it is a state this product can currently reach,
 * and the page that shows it is how somebody notices.
 */
public record PositionDetailResponse(

        String positionDocsId,
        String title,
        String departmentDocsId,

        /** The unit's name, so the page has a heading without a second call. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String departmentName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reportsToPositionDocsId,

        /** The title of the seat this one reports to. Absent when it reports to nobody. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reportsToPositionTitle,

        Integer approvedHeadcount,

        /** {@code holders.size()}. See the class note on why it is not a separate query. */
        long filledHeadcount,

        /** Approved minus filled, floored at zero. */
        long vacancies,

        /** Whether more people hold it than were approved. #16 warns rather than refusing. */
        boolean overFilled,

        Boolean teachingPosition,
        Boolean active,

        /** Who is in it now, earliest start first — longest-serving at the top. */
        List<PositionHolderResponse> holders,

        /** Why the list is empty. Present only when it is. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String holdersNote,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note) {
}
