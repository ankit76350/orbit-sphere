package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.AttendanceSource;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

/**
 * What attendance #6 answers: one register, and <b>every child who belongs in it</b> — the ones
 * whose placement covered the register's date — each with their mark, or none yet.
 */
public record AttendanceRegisterResponse(

        /** The register itself. */
        AttendanceSessionResponse session,

        /** Children in the section on that date. */
        int studentCount,

        /** How many of them have a mark. */
        long markedCount,

        /** How many do not have one yet. */
        long unmarkedCount,

        /** Marks per status — every status listed, zeros included. */
        Map<String, Long> markCounts,

        /** The children, by roll number then name. */
        List<StudentRow> students,

        String nextStep) {

    /** One child in the register. */
    public record StudentRow(

            String studentDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String fullName,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String admissionNo,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String rollNo,

            /** The placement the child was in that day — what a mark is stored against. */
            String studentAcademicRecordDocsId,

            /**
             * False for a child who has a mark but is no longer in the section for that date —
             * their placement was corrected after the register was taken. Kept so no mark is
             * hidden.
             */
            boolean onRoster,

            /** Their mark, or absent when they have not been marked yet. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Mark mark) {
    }

    /** One child's mark. */
    public record Mark(

            AttendanceStatus status,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String reason,

            AttendanceSource source,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            Instant arrivalAt,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            Instant departureAt) {
    }
}
