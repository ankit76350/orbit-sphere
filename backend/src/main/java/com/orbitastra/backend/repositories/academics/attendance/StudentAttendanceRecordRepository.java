package com.orbitastra.backend.repositories.academics.attendance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.attendance.StudentAttendanceRecord;

/**
 * One child's mark in one attendance register.
 *
 * <p>Only what attendance #6 needs so far. Marking (#2) and the reports (#8 to #11) will add
 * their own reads when they are built.
 */
public interface StudentAttendanceRecordRepository
        extends MongoRepository<StudentAttendanceRecord, String>,
        StudentAttendanceRecordRepositoryCustom {

    /** Every mark in one register — what #6 lays beside the children. */
    List<StudentAttendanceRecord> findBySchoolIdAndAttendanceSessionDocsId(String schoolId,
            String attendanceSessionDocsId);
}
