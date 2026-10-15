package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;

/**
 * One attendance register, with the class and the teacher named.
 */
public record AttendanceSessionResponse(

        String attendanceSessionDocsId,

        String academicYear,

        LocalDate attendanceDate,

        String classDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,

        AttendanceSessionType sessionType,

        /** Only on a PERIOD register. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String periodCode,

        /** The timetable period this register was opened for. Only on a PERIOD register. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String timetableEntryDocsId,

        /** Who takes the register. Absent when nobody could be worked out. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherName,

        AttendanceSessionStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant lockedAt,

        /** True when a CANCELLED register was opened again rather than a new one created. */
        boolean reopened,

        /** For the next write to this register — lock, unlock and cancel all want it. */
        Long version,

        String nextStep) {

    /** Builds the answer from the saved register and the two names behind its ids. */
    public static AttendanceSessionResponse of(AttendanceSession session, String className,
            String teacherName, boolean reopened, String nextStep) {

        return new AttendanceSessionResponse(
                session.getId(),
                session.getAcademicYear(),
                session.getAttendanceDate(),
                session.getClassDocsId(),
                className,
                session.getSectionNo(),
                session.getSessionType(),
                session.getPeriodCode(),
                session.getTimetableEntryDocsId(),
                session.getTeacherDocsId(),
                teacherName,
                session.getStatus(),
                session.getLockedAt(),
                reopened,
                session.getVersion(),
                nextStep);
    }
}
