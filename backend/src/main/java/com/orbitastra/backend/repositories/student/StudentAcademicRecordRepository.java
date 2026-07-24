package com.orbitastra.backend.repositories.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.StudentStatus;

@Repository
public interface StudentAcademicRecordRepository extends MongoRepository<StudentAcademicRecord, String> {
    List<StudentAcademicRecord> findByStudentDocsId(String studentDocsId);
    List<StudentAcademicRecord> findByStudentDocsIdIn(List<String> studentDocsIds);
    Optional<StudentAcademicRecord> findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
            String studentDocsId, String academicYear, StudentStatus status);
    Optional<StudentAcademicRecord> findFirstBySchoolIdAndAcademicYearAndIdentityNoAndStatus(
            String schoolId, String academicYear, String identityNo, StudentStatus status);
    Optional<StudentAcademicRecord> findFirstByClassDocsIdAndSectionNoAndAcademicYearAndRollNoAndStatus(
            String classDocsId, String sectionNo, String academicYear, String rollNo, StudentStatus status);
    List<StudentAcademicRecord> findByClassDocsIdAndStatus(
            String classDocsId, StudentStatus status);
    List<StudentAcademicRecord> findByClassDocsIdAndAcademicYearAndStatus(
            String classDocsId, String academicYear, StudentStatus status);
    List<StudentAcademicRecord> findByHostelRoomNoAndStatus(
            String hostelRoomNo, StudentStatus status);
    List<StudentAcademicRecord> findBySchoolId(String schoolId);
    List<StudentAcademicRecord> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    boolean existsByStudentDocsIdAndClassDocsId(String studentDocsId, String classDocsId);
}
