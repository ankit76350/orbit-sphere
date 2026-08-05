package com.orbitastra.backend.models.new_new.academics.attendance;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.AttendanceSource;
import com.orbitastra.backend.models.new_new.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One student's mark in one AttendanceSession.
 *
 * <p>Who marked the student and when are the inherited {@code createdByDocsId}
 * and {@code createdAt}; a later correction updates {@code updatedByDocsId} and
 * {@code updatedAt}. No separate recorded-by or recorded-at field is kept, so
 * MongoDB auditing and an {@code AuditorAware} must be configured for this
 * collection to be traceable at all.
 *
 * <p>One consequence: for an offline app that syncs later, or a bulk import of a
 * paper register, {@code createdAt} is the time the document was written rather
 * than the time attendance was taken. {@code attendanceDate} remains the
 * authoritative business date, and {@code source} records that the mark did not
 * come from a live register.
 *
 * <p>The unique index on session and student makes a repeated biometric scan or
 * a re-run import an idempotent upsert rather than a duplicate row.
 */
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

    // Links to the student's StudentAcademicRecord.id for this year. Kept as
    // recorded, so a mid-year class or section change does not rewrite history.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // Copied from AttendanceSession for student-history queries. Example: 2026-06-05
    @NotNull
    private LocalDate attendanceDate;

    // Example: AttendanceStatus.AUTHORISED_ABSENCE
    @NotNull
    private AttendanceStatus status;

    // School-defined detail behind the status. Required whenever status is not
    // PRESENT, including LATE. Example: "MEDICAL"
    private String reason;

    // Example: AttendanceSource.MANUAL
    @NotNull
    @Builder.Default
    private AttendanceSource source = AttendanceSource.MANUAL;

    // Optional observed arrival time, used with AttendanceStatus.LATE.
    // Example: 2026-06-05T03:02:00Z
    private Instant arrivalAt;

    // Optional observed departure time, used when a student leaves early.
    // Example: 2026-06-05T10:30:00Z
    private Instant departureAt;

    // Whether a guardian has been told about this mark. Per-channel delivery
    // state belongs to the future communication module; this flag only drives
    // the follow-up queue. Example: false
    @NotNull
    @Builder.Default
    private Boolean guardianNotified = false;

    // Set when a guardian confirms they saw the notification.
    // Example: 2026-06-05T04:00:00Z
    private Instant guardianAcknowledgedAt;

    /**
     * Every mark other than PRESENT must carry a reason.
     *
     * <p>An absence or a late arrival with no recorded explanation cannot be
     * defended to a guardian or an inspector afterwards, and the reason cannot be
     * reconstructed once the day has passed. A blank string does not satisfy the
     * rule; the service must normalize blanks to null.
     *
     * <p>A null status is treated as satisfied so this does not duplicate the
     * {@code @NotNull} failure already reported on {@code status}.
     *
     * <p>This is a derived check, not stored data. Request DTOs should apply the
     * same rule so the API returns a field-level message on the offending
     * property.
     */
    @Transient
    @AssertTrue(message = "reason is required when status is not PRESENT")
    public boolean isReasonSuppliedWhenRequired() {
        return status == null
                || status == AttendanceStatus.PRESENT
                || (reason != null && !reason.isBlank());
    }
}
