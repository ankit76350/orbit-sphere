package com.orbitastra.backend.dto.people.organization.request;

/**
 * What #15 filters a school's seats by.
 *
 * <p><b>Every field is optional and absent means "do not filter on this".</b> Absent is not the
 * same as {@code false}: {@code ?active=} left off returns retired seats as well as active ones,
 * which is the difference between "the whole chart" and "what is still open".
 *
 * <p><b>{@code vacant} is the one parameter here that is not answered by the positions
 * collection.</b> Whether a seat is vacant depends on how many current employment records name
 * it, which lives in another collection and is never stored on the seat — see
 * {@link com.orbitastra.backend.services.people.OrganizationService#listPositions} for what that
 * costs, because it is the one filter that cannot be applied before paging.
 */
public record PositionSearchRequest(

        /** The seats of one department. Absent returns every department's. */
        String departmentDocsId,

        /** Seats in use, or retired ones. Absent returns both. */
        Boolean active,

        /** Teaching seats, or non-teaching ones. Absent returns both. */
        Boolean teaching,

        /**
         * Seats with approved headcount left to fill.
         *
         * <p><b>Computed, not stored</b> — {@code filledHeadcount < approvedHeadcount}. This is
         * the query a school runs at the start of a hiring round, and the reason #15 exists at
         * all rather than being a field on #52.
         */
        Boolean vacant,

        /** Case-insensitive, matches anywhere in `title`. Blank is absent. */
        String search,

        Integer page,
        Integer size,
        String sort) {
}
