package com.orbitastra.backend.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.AcademicResult;

@Repository
public interface AcademicResultRepository extends MongoRepository<AcademicResult, String> {
    List<AcademicResult> findBySchoolId(String schoolId);
    List<AcademicResult> findByStudentId(String studentId);
    List<AcademicResult> findBySchoolIdAndStudentId(String schoolId, String studentId);
    List<AcademicResult> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<AcademicResult> findByStudentIdAndAcademicYear(String studentId, String academicYear);
}
