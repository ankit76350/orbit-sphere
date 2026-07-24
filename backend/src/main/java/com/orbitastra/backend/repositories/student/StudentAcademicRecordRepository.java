package com.orbitastra.backend.repositories.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.student.StudentAcademicRecord;

@Repository
public interface StudentAcademicRecordRepository extends MongoRepository<StudentAcademicRecord, String> {
    List<StudentAcademicRecord> findByStudentDocsId(String studentDocsId);
    List<StudentAcademicRecord> findByStudentDocsIdIn(List<String> studentDocsIds);
    Optional<StudentAcademicRecord> findByStudentDocsIdAndAcademicYear(String studentDocsId, String academicYear);
    List<StudentAcademicRecord> findByClassDocsId(String classDocsId);
    List<StudentAcademicRecord> findByClassDocsIdAndAcademicYear(String classDocsId, String academicYear);
    List<StudentAcademicRecord> findByHostelRoomNo(String hostelRoomNo);
    List<StudentAcademicRecord> findByHostelRoomNoAndAcademicYear(String hostelRoomNo, String academicYear);
    List<StudentAcademicRecord> findBySchoolId(String schoolId);
    List<StudentAcademicRecord> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    boolean existsByStudentDocsIdAndClassDocsId(String studentDocsId, String classDocsId);
}
