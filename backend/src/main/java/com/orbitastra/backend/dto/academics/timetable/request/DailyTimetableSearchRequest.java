package com.orbitastra.backend.dto.academics.timetable.request;

import java.time.LocalDate;

/**
 * What #10 filters a year's timetables by.
 *
 * <p><b>Every field is optional and absent means "do not filter on this".</b> Sent with nothing at
 * all, this is "every day this year has a timetable for", newest work first.
 *
 * <p><b>The academic year is not here.</b> It comes from the path, like #1's — a timetable belongs
 * to a year, and a filter a caller can leave off is not the boundary of what may be read.
 *
 * <p><b>{@code classDocsId} and {@code sectionNo} are matched together, not separately.</b> A day
 * holds every class's periods, so asking for class X and section A without pairing them would
 * match a day where one entry belongs to X and an unrelated entry belongs to some section A —
 * which is nearly every day. See the repository, where that becomes an {@code $elemMatch}.
 */
public record DailyTimetableSearchRequest(

        /** The first date to include. Absent starts at the beginning of the year. */
        LocalDate from,

        /** The last date to include, inclusive. Absent runs to the end of the year. */
        LocalDate to,

        /** Days on which this class is taught at all. */
        String classDocsId,

        /**
         * Days on which this section has periods.
         *
         * <p>Only meaningful beside {@code classDocsId} — two classes each have a section "A", and
         * a section number alone names both. Sent alone it still filters, and the repository says
         * what that means.
         */
        String sectionNo,

        /** Days this teacher works — the query a staff page asks. */
        String teacherDocsId,

        /** Days on which this room is used. */
        String facilityResourceDocsId,

        Integer page,
        Integer size,
        String sort) {
}
