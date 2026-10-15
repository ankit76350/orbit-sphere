package com.orbitastra.backend.repositories.academics.attendance;

import java.util.Collection;
import java.util.List;

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
}
