package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One teacher's day — what a teacher's app opens. Endpoint #9.
 *
 * <h2>Sorted by time, for the same reason #8 is</h2>
 *
 * <p>A teacher cannot be in two places at once — {@code TEACHER_PERIOD_OVERLAP} is refused on every
 * write — so within one person's day {@code startTime} is a <i>total</i> order. That is what makes
 * sorting meaningful here and a guess in #7, which returns the whole school's periods in stored
 * order.
 *
 * <h2>A break they supervise is part of their day</h2>
 *
 * <p>Since 2026-09-17 a non-lesson may carry a {@code teacherDocsId}: somebody supervises lunch,
 * runs the assembly, takes the activity. Those periods appear here, and they are the reason
 * {@code lessonCount} and {@code entryCount} differ — <b>a teacher with no lessons can still have a
 * working day</b>.
 *
 * <h2>{@code firstStartTime} and {@code lastEndTime}, and what they are not</h2>
 *
 * <p>They are the ends of the day this person is committed to, which is what an app puts at the top
 * of the screen. <b>They are not "free from" and "free until"</b>: the gaps between periods are not
 * computed here, and #12 — who can cover this period — is the endpoint that answers that question
 * properly, against every member of staff rather than one.
 */
public record TeacherDayResponse(

        LocalDate date,
        String academicYear,
        String dailyTimetableDocsId,

        String teacherDocsId,

        /** Their name, as staff records it. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherName,

        /** How many periods they are named on that day. Zero is a normal answer. */
        int entryCount,

        /** How many of those they teach. The rest are breaks and duties. */
        int lessonCount,

        /** Distinct class-and-section pairs they see. "A" of one class and "A" of another are two. */
        int sectionCount,

        /** When their first period starts. Absent when they have nothing that day. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalTime firstStartTime,

        /** When their last period ends. Absent when they have nothing that day. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalTime lastEndTime,

        /** Their periods, earliest first. */
        List<TimetableEntryDetailResponse> entries,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
