package com.orbitastra.backend.repositories.academics.attendance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.orbitastra.backend.models.academics.attendance.StudentAttendanceRecord;

/** The attendance-mark reads a derived query cannot express. */
public interface StudentAttendanceRecordRepositoryCustom {

    /**
     * #7 — how many marks of each status every one of these registers holds, in one
     * aggregation for the whole page.
     */
    List<SessionStatusCount> countBySessionAndStatus(String schoolId,
            Collection<String> attendanceSessionDocsIds);

    /** One group: a register, a mark status, and how many children have it. */
    record SessionStatusCount(String attendanceSessionDocsId, String status, long count) {
    }

    /**
     * #9 — every mark one child has in a year between two dates, <b>both ends included</b>,
     * newest first. Served by {@code school_student_attendance_history_idx}.
     */
    List<StudentAttendanceRecord> findForStudent(String schoolId, String studentDocsId,
            String academicYear, LocalDate from, LocalDate to);

    /**
     * #10 — how many marks of each status every child holds across these registers, in one
     * aggregation.
     */
    List<StudentStatusCount> countByStudentAndStatus(String schoolId,
            Collection<String> attendanceSessionDocsIds);

    /** One group: a child, a mark status, and how many registers gave them it. */
    record StudentStatusCount(String studentDocsId, String status, long count) {
    }
}
