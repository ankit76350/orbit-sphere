package com.orbitastra.backend.dto.academics.attendance.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What attendance #1 needs to open a register for one section on one date.
 *
 * <p>The year is in the path. <b>{@code timetableEntryDocsId} is not sent</b>: for a
 * {@code PERIOD} register it is copied from the timetable period, so it can never disagree with
 * it.
 */
public record AttendanceSessionCreateRequest(

        /** The day the register is for. Must be inside the year, and a working day. */
        @NotNull LocalDate attendanceDate,

        /** A class of the year in the path. */
        @NotBlank @Size(max = 60) String classDocsId,

        /** A section of that class, and active. */
        @NotBlank @Size(max = 20) String sectionNo,

        /** {@code DAILY} — once for the day — or {@code PERIOD} — one timetable period. */
        @NotNull AttendanceSessionType sessionType,

        /**
         * <b>Required for {@code PERIOD}, refused for {@code DAILY}.</b> Must be a period that
         * section has in that day's timetable.
         */
        @Size(max = 20) String periodCode,

        /**
         * Who takes the register. <b>Absent:</b> the period's teacher for {@code PERIOD}, the
         * section's class teacher for {@code DAILY}. Sent: must be staff of this school — a
         * substitute taking somebody else's period, say.
         */
        @Size(max = 60) String teacherDocsId) {
}
