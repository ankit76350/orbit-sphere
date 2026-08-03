package com.orbitastra.backend.models.new_new.academics.attendance;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.AttendanceSource;
import com.orbitastra.backend.models.new_new.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One student's row in one AttendanceSession. */
@Document(collection = "student_attendance_records")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_session_student_attendance_uniq",
                def = "{'schoolId': 1, 'attendanceSessionDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_student_attendance_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'attendanceDate': -1, 'status': 1}"),
        @CompoundIndex(
                name = "school_date_attendance_status_idx",
                def = "{'schoolId': 1, 'attendanceDate': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceRecord extends AcademicStudentSchoolBase {

    // Links to AttendanceSession.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String attendanceSessionDocsId;

    // Links to the student's StudentAcademicRecord.id for this year.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // Copied from AttendanceSession for student-history queries. Example: 2026-06-05
    @NotNull
    private LocalDate attendanceDate;

    // Example: AttendanceStatus.PRESENT
    @NotNull
    private AttendanceStatus status;

    // School-defined reason code. Example: "MEDICAL_LEAVE"
    private String reasonCode;

    // Example: AttendanceSource.MANUAL
    @NotNull
    @Builder.Default
    private AttendanceSource source = AttendanceSource.MANUAL;

    // Links to the Staff.id that recorded or corrected the row.
    // Example: "67aa15d9dc3f7d0033333333"
    private String recordedByDocsId;

    // Example: 2026-06-05T03:05:00Z
    @NotNull
    private Instant recordedAt;

    // Optional observed arrival time. Example: 2026-06-05T03:02:00Z
    private Instant arrivalAt;

    // Optional observed departure time. Example: 2026-06-05T10:30:00Z
    private Instant departureAt;

    // Example: false
    @NotNull
    @Builder.Default
    private Boolean guardianNotified = false;

    // Example: 2026-06-05T04:00:00Z
    private Instant guardianAcknowledgedAt;
}
