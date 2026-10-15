package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;

/**
 * What attendance #8 answers: <b>for one date, which sections have taken attendance and which
 * have not</b>. "Taken" means the section's DAILY register exists and is not cancelled; period
 * registers are counted beside it but do not make a section "taken".
 */
public record AttendanceDayResponse(

        String academicYear,

        LocalDate date,

        /** Active sections of active classes in the year. */
        int sectionCount,

        /** Sections whose DAILY register exists and is not cancelled. */
        int takenCount,

        /** Sections with no DAILY register, or only a cancelled one. */
        int notTakenCount,

        /** Of the taken ones, how many are locked — final. */
        int lockedCount,

        /** One row per section, by class name then section. */
        List<SectionRow> sections) {

    /** One section on that date. */
    public record SectionRow(

            String classDocsId,

            String className,

            String sectionNo,

            /** Children whose placement covered the date. */
            long studentCount,

            /** True when the DAILY register exists and is not cancelled. */
            boolean taken,

            /** The DAILY register, when there is one — including a cancelled one. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String attendanceSessionDocsId,

            /** Its status, when there is one. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            AttendanceSessionStatus status,

            /** Children marked in the DAILY register. */
            long markedCount,

            /** Children in the section with no mark in it — all of them when it is not taken. */
            long unmarkedCount,

            /** DAILY-register marks per status, every status listed. */
            Map<String, Long> markCounts,

            /** PERIOD registers opened for this section that day, cancelled ones left out. */
            int periodRegisterCount) {
    }
}
