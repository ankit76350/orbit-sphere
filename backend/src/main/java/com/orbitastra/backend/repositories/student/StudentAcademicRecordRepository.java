package com.orbitastra.backend.repositories.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.student.StudentAcademicRecord;

@Repository
public interface StudentAcademicRecordRepository extends MongoRepository<StudentAcademicRecord, String> {
    List<StudentAcademicRecord> findByStudentDocId(String studentDocId);
    Optional<StudentAcademicRecord> findByStudentDocIdAndAcademicYearId(String studentDocId, String academicYearId);
    List<StudentAcademicRecord> findByClassId(String classId);
    List<StudentAcademicRecord> findByClassIdAndAcademicYearId(String classId, String academicYearId);
    List<StudentAcademicRecord> findByHostelRoomId(String hostelRoomId);
    List<StudentAcademicRecord> findByHostelRoomIdAndAcademicYearId(String hostelRoomId, String academicYearId);
    List<StudentAcademicRecord> findBySchoolId(String schoolId);
    List<StudentAcademicRecord> findBySchoolIdAndAcademicYearId(String schoolId, String academicYearId);
}
