package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;

/**
 * One day as a row on #10's page — what it is, not what is in it.
 *
 * <h2>The counts are computed in the database, and the periods never leave it</h2>
 *
 * <p>A school day measures about 120 KB once it is full. A page of twenty of them is two and a
 * half megabytes of embedded periods shipped to render twenty dates and five numbers — so the
 * counts are worked out by an aggregation and the {@code entries} array is never read.
 *
 * <p>That is the same call #6 of grading makes about bands and #7 of positions makes about
 * holders: <b>a list carries what a list needs, and one call away is the endpoint that carries the
 * rest.</b> Here that is #7, {@code GET /timetables/{date}}.
 *
 * <h2>Why these five</h2>
 *
 * <p>They are the questions a person asks of a list of days without opening one: how full is it,
 * how much of it is teaching, how much of the school it covers, and how many people it takes.
 * {@code sectionCount} counts <b>class and section together</b>, because "section A" is not one
 * thing across a school.
 */
public record DailyTimetableSummaryResponse(

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
        int teacherCount) {
}
