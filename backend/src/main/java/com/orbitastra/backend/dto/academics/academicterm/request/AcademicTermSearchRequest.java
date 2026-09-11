package com.orbitastra.backend.dto.academics.academicterm.request;

import java.time.LocalDate;

/**
 * What #9 will filter a year's terms by.
 *
 * <p><b>Every field is optional and absent means "do not filter on this".</b> Absent is not the
 * same as {@code false}: {@code ?active=} left off returns retired terms as well as active ones,
 * which is the difference between "show me everything" and "show me what is retired".
 *
 * <p><b>The tenant and the year are not here.</b> They come from the header and the path and are
 * passed to the repository separately, because they are the boundary of what may be read rather
 * than a filter a caller chose. A filter a caller can send is one a caller can leave off.
 */
public record AcademicTermSearchRequest(

        /** Terms in use, or retired ones. Absent returns both. */
        Boolean active,

        /** Case-insensitive, matches anywhere in `name` OR `termCode`. Blank is treated as absent. */
        String search,

        /** Terms whose results are frozen, or not. The operational question #5 and #6 answer. */
        Boolean resultsLocked,

        /** Terms that carry a `weightPercent`, or those that do not. */
        Boolean weighted,

        /**
         * The term covering one date — {@code startDate <= coversDate <= endDate}, both inclusive.
         *
         * <p>Distinct from #10, which answers "the term covering <i>today</i>, in the school's own
         * time zone". This asks about a date the caller names, so no zone is involved and no
         * "today" has to be agreed on.
         */
        LocalDate coversDate,

        Integer page,
        Integer size,
        String sort) {
}
