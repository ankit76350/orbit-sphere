package com.orbitastra.backend.repositories.academics.attendance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionSearchRequest;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;

/** The attendance-register reads a derived query cannot express. */
public interface AttendanceSessionRepositoryCustom {

    /** #7 — one page of a year's registers, with whichever filters were sent. */
    Page<AttendanceSession> search(String schoolId, String academicYear,
            AttendanceSessionSearchRequest request, Pageable pageable);

    /**
     * #10 — one section's DAILY registers between two dates, both ends included, cancelled ones
     * left out, oldest first.
     */
    List<AttendanceSession> findDailyForSection(String schoolId, String academicYear,
            String classDocsId, String sectionNo, LocalDate from, LocalDate to);
}
