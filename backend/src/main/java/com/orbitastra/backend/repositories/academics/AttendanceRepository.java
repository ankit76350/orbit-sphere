package com.orbitastra.backend.repositories.academics;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.Attendance;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    List<Attendance> findBySchoolId(String schoolId);
    List<Attendance> findByStudentId(String studentId);
    List<Attendance> findByStudentIdAndDate(String studentId, LocalDate date);
    List<Attendance> findBySchoolIdAndDate(String schoolId, LocalDate date);
    List<Attendance> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<Attendance> findByStudentIdAndAcademicYear(String studentId, String academicYear);
}
