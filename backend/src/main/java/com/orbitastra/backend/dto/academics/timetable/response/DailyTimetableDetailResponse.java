package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One school day in full — what #7 answers, and the other half of #10.
 *
 * <h2>The same five counts as a row of #10</h2>
 *
 * <p>They are repeated rather than assumed to be in hand. A caller reaching a day by <b>link</b> —
 * a bookmark, a shared URL, an attendance record pointing back at the date it was taken on — never
 * saw the list, and a screen that could only show a heading when it had been arrived at from
 * somewhere else would be a screen that is sometimes broken.
 *
 * <p>They cost nothing here: the entries are already loaded, so the counts are worked out in
 * memory rather than by the aggregation #10 needs.
 *
 * <h2>Entries in stored order, never re-sorted</h2>
 *
 * <p>The order a day was written in is a fact about it, and the only one the server can state
 * without inventing a rule. <b>Sorting by time would be the obvious choice and is the wrong one</b>
 * — periods of different sections run at the same hour, so "by time" is not an order, it is a tie
 * with a hidden second key. Which grouping a screen wants — by section, by teacher, by hour — is
 * the screen's question, and #8, #9 and #11 are the endpoints that answer it for one of each.
 */
public record DailyTimetableDetailResponse(

        String dailyTimetableDocsId,
        LocalDate date,
        String academicYear,

        /** Every period of every section on that date. */
        int entryCount,

        /** How many of those are taught. The rest are breaks, assemblies and activities. */
        int lessonCount,

        /** Distinct classes with anything scheduled. */
        int classCount,

        /** Distinct class-and-section pairs — "A" of one class and "A" of another are two. */
        int sectionCount,

        /** Distinct staff named anywhere on the day, supervising a break included. */
        int teacherCount,

        /** Every period, in the order it was written, with the names behind its ids. */
        List<TimetableEntryDetailResponse> entries,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
