package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What attendance #10 answers: <b>the monthly register</b> of one section — one row per child who
 * was in it at any time in the range, with their counts and percentage.
 *
 * <p>The same rules as #9: <b>DAILY registers only</b>, <b>LATE counts as attended</b>, cancelled
 * registers left out.
 */
public record SectionAttendanceResponse(

        String academicYear,

        String classDocsId,

        String className,

        String sectionNo,

        LocalDate from,

        LocalDate to,

        /** DAILY registers held for this section in the range. */
        int registerCount,

        /** Children in the section at any time in the range. */
        int studentCount,

        /** One row per child, by roll number then name. */
        List<StudentRow> students) {

    /** One child's attendance in this section over the range. */
    public record StudentRow(

            String studentDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String fullName,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String admissionNo,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String rollNo,

            /** When their placement in the section started. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            LocalDate placedFrom,

            /** When it ended, when it has. Absent while they are still in it. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            LocalDate placedUntil,

            /** Registers held while they were in the section — the days they could be marked. */
            long registerDays,

            long markedDays,

            /** PRESENT + LATE. */
            long attendedDays,

            /** registerDays they have no mark in. */
            long unmarkedDays,

            /** Their marks per status — every status listed, zeros included. */
            Map<String, Long> counts,

            /** attendedDays out of markedDays, to one decimal. Absent when nothing is marked. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Double attendancePercentage) {
    }
}
