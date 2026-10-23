package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One section's day — what a child's parent opens. Endpoint #8.
 *
 * <h2>Sorted by time, unlike #7</h2>
 *
 * <p>#7 returns the whole school's day in <b>stored order</b> and says why: periods of different
 * sections run at the same hour, so "by time" there is not an order at all, it is a tie with a
 * hidden second key.
 *
 * <p><b>That objection does not apply to one section.</b> A section cannot be in two places at once
 * — {@code SECTION_PERIOD_OVERLAP} is refused on every write — so within one section
 * {@code startTime} is a <i>total</i> order, and it is the order a parent reads the day in. Sorting
 * here is meaningful where sorting there would have been a guess.
 *
 * <h2>An empty day is a 200, not a 404</h2>
 *
 * <p>The date has a timetable and this section has nothing in it: that is a fact about the section,
 * not a missing document. The three 404s belong to the <i>day</i>, and they are the same three #7
 * gives. A class or section that does not exist is refused separately, so "nothing scheduled" and
 * "you asked for the wrong section" never look alike.
 */
public record SectionDayResponse(

        LocalDate date,
        String academicYear,
        String dailyTimetableDocsId,

        String classDocsId,

        /** What the school calls that class. Absent only if the class has since been deleted. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        /** The class's own spelling of the section, whatever case the caller asked in. */
        String sectionNo,

        /** How many periods this section has that day. Zero is a normal answer. */
        int entryCount,

        /** How many of those are taught. The rest are breaks, assemblies and activities. */
        int lessonCount,

        /** Distinct staff this section sees that day, a break's supervisor included. */
        int teacherCount,

        /** This section's periods, earliest first. */
        List<TimetableEntryDetailResponse> entries,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
