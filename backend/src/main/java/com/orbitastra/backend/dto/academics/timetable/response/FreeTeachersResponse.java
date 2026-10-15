package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * What #12 answers: the staff who have nothing on the timetable during one window of one date —
 * who could cover a period.
 *
 * <p><b>"Free" means "no period overlapping this window", and nothing more.</b> It cannot mean "at
 * work today": leave is not built, so somebody on leave reads as free. {@code nextStep} says so.
 */
public record FreeTeachersResponse(

        LocalDate date,

        String academicYear,

        String dailyTimetableDocsId,

        LocalTime startTime,

        LocalTime endTime,

        /** Every staff member the school has — the pool "free" was chosen from. */
        int staffCount,

        /** How many of them have nothing in the window. */
        int freeCount,

        /** The free staff, least busy first. */
        List<FreeTeacherRow> teachers,

        String nextStep) {

    /**
     * One free member of staff, and how busy their day already is.
     *
     * <p>"Free" and "free, and already teaching six periods" are different answers, and only the
     * school can weigh them — so both counts are here.
     */
    public record FreeTeacherRow(

            String staffDocsId,

            String fullName,

            String employeeNo,

            /** Every period they have that date, supervised breaks included. */
            int periodCount,

            /** Only the lessons among them. */
            int lessonCount) {
    }
}
