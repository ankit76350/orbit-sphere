package com.orbitastra.backend.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.AcademicResult;

@Repository
public interface AcademicResultRepository extends MongoRepository<AcademicResult, String> {
    List<AcademicResult> findBySchoolId(String schoolId);
    List<AcademicResult> findByStudentDocsId(String studentDocsId);
    List<AcademicResult> findBySchoolIdAndStudentDocsId(String schoolId, String studentDocsId);
    List<AcademicResult> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<AcademicResult> findByStudentDocsIdAndAcademicYear(String studentDocsId, String academicYear);
}
