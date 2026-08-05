package com.orbitastra.backend.models.new_new.academics.attendance;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.new_new.academics.enums.AttendanceSessionType;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One editable attendance register for a class section on a date.
 *
 * <p>It may represent daily attendance or one timetable period. Student rows
 * are stored separately in StudentAttendanceRecord.
 */
@Document(collection = "attendance_sessions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_attendance_session_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'attendanceDate': 1, 'classDocsId': 1, 'sectionCode': 1, 'sessionType': 1, 'periodCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_attendance_date_status_idx",
                def = "{'schoolId': 1, 'attendanceDate': 1, 'status': 1, 'classDocsId': 1, 'sectionCode': 1}"),
        @CompoundIndex(
                name = "school_attendance_opened_by_idx",
                def = "{'schoolId': 1, 'openedByDocsId': 1, 'attendanceDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Example: 2026-06-05
    @NotNull
    private LocalDate attendanceDate;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // References an embedded SchoolClass.sections[].sectionCode. Example: "A"
    @NotBlank
    private String sectionCode;

    // Example: AttendanceSessionType.DAILY
    @NotNull
    private AttendanceSessionType sessionType;

    // Required for PERIOD sessions. Example: "P03"
    private String periodCode;

    // Optionally references SchoolClass.subjects[].subjectCode for a PERIOD session.
    // Example: "MATHEMATICS"
    private String subjectCode;

    // Optionally links to DailyTimetable.id for a PERIOD session.
    // Example: "67aa15d9dc3f7d0044444444"
    private String dailyTimetableDocsId;

    // Optionally references an embedded DailyTimetable.entries[].entryId.
    // Example: "entry_000124"
    private String timetableEntryId;

    // Example: AttendanceSessionStatus.OPEN
    @NotNull
    @Builder.Default
    private AttendanceSessionStatus status = AttendanceSessionStatus.OPEN;

    // Links to the Staff.id that opened the register.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String openedByDocsId;

    // Example: 2026-06-05T03:00:00Z
    @NotNull
    private Instant openedAt;

    // Example: 2026-06-05T03:20:00Z
    private Instant lockedAt;

    // Links to the Staff.id that locked the register.
    // Example: "67aa15d9dc3f7d0055555555"
    private String lockedByDocsId;
}
