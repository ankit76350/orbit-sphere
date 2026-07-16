package com.orbitastra.backend.repositories.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.student.StudentAcademicRecord;

@Repository
public interface StudentAcademicRecordRepository extends MongoRepository<StudentAcademicRecord, String> {
    List<StudentAcademicRecord> findByStudentDocId(String studentDocId);
    List<StudentAcademicRecord> findByStudentDocIdIn(List<String> studentDocIds);
    Optional<StudentAcademicRecord> findByStudentDocIdAndAcademicYear(String studentDocId, String academicYear);
    List<StudentAcademicRecord> findByClassDocId(String classDocId);
    List<StudentAcademicRecord> findByClassDocIdAndAcademicYear(String classDocId, String academicYear);
    List<StudentAcademicRecord> findByHostelRoomId(String hostelRoomId);
    List<StudentAcademicRecord> findByHostelRoomIdAndAcademicYear(String hostelRoomId, String academicYear);
    List<StudentAcademicRecord> findBySchoolId(String schoolId);
    List<StudentAcademicRecord> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    boolean existsByStudentDocIdAndClassDocId(String studentDocId, String classDocId);
}
