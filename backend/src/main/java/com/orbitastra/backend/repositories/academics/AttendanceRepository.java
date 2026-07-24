package com.orbitastra.backend.repositories.academics;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.Attendance;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    List<Attendance> findBySchoolId(String schoolId);
    List<Attendance> findByStudentDocsId(String studentDocsId);
    List<Attendance> findByStudentDocsIdAndDate(String studentDocsId, LocalDate date);
    List<Attendance> findBySchoolIdAndDate(String schoolId, LocalDate date);
    List<Attendance> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<Attendance> findByStudentDocsIdAndAcademicYear(String studentDocsId, String academicYear);
    boolean existsByStudentDocsIdAndDate(String studentDocsId, LocalDate date);
    boolean existsByStudentDocsIdAndDateAndIdNot(String studentDocsId, LocalDate date, String id);
}
