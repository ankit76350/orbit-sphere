package com.orbitastra.backend.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.DisciplineLog;

@Repository
public interface DisciplineLogRepository extends MongoRepository<DisciplineLog, String> {
    List<DisciplineLog> findBySchoolId(String schoolId);
    List<DisciplineLog> findByStudentId(String studentId);
    List<DisciplineLog> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<DisciplineLog> findByStudentIdAndAcademicYear(String studentId, String academicYear);
}
