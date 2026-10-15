package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

/**
 * What attendance #9 answers: <b>one child's attendance</b> between two dates — every mark, and
 * the totals.
 *
 * <p><b>The totals count DAILY registers only</b> (open item 8); period marks are listed but do not
 * change the percentage. <b>LATE counts as attended</b> (open item 7) and is also counted on its
 * own. Marks in cancelled registers are left out entirely.
 */
public record StudentAttendanceResponse(

        String studentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String studentName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        String academicYear,

        LocalDate from,

        LocalDate to,

        /** DAILY marks per status — every status listed, zeros included. */
        Map<String, Long> dailyCounts,

        /** Days with a DAILY mark. */
        long markedDays,

        /** Of those, days PRESENT or LATE. */
        long attendedDays,

        /** attendedDays out of markedDays, as a percentage to one decimal. Absent when no day is marked. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Double attendancePercentage,

        /** Period marks in the range — listed below, not counted in the percentage. */
        long periodMarkCount,

        /** Every mark, newest date first. */
        List<Row> records) {

    /** One mark, with the register it is in. */
    public record Row(

            LocalDate attendanceDate,

            AttendanceSessionType sessionType,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String periodCode,

            String classDocsId,

            String sectionNo,

            AttendanceStatus status,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String reason,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            Instant arrivalAt,

            String attendanceSessionDocsId,

            /** The register's own status — OPEN or LOCKED. */
            AttendanceSessionStatus registerStatus) {
    }
}
