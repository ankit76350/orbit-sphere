package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

/**
 * What attendance #11 answers: <b>the absentees of one date</b>, for follow-up — every child
 * marked absent in a DAILY register, and whether their guardian has been told.
 *
 * <p>The notification flags are read here, never written: telling a guardian is the notification
 * system's job.
 */
public record AbsenteesResponse(

        String academicYear,

        LocalDate date,

        /** The guardianNotified filter that was applied, when one was. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean guardianNotified,

        /** Children absent that day. */
        int absentCount,

        int authorisedCount,

        int unauthorisedCount,

        /** Of the absent, how many guardians have not been told yet. */
        int notNotifiedCount,

        /** One row per absent child, by class, section, then name. */
        List<Row> absentees) {

    /** One absent child. */
    public record Row(

            String studentDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String fullName,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String admissionNo,

            String classDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String className,

            String sectionNo,

            /** AUTHORISED_ABSENCE or UNAUTHORISED_ABSENCE. */
            AttendanceStatus status,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String reason,

            /** Whether a guardian has been told about this absence. */
            boolean guardianNotified,

            /** When a guardian confirmed they saw it. Absent until they do. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Instant guardianAcknowledgedAt,

            String attendanceSessionDocsId,

            AttendanceSessionStatus registerStatus) {
    }
}
