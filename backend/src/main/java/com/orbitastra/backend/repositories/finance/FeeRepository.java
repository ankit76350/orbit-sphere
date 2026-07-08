package com.orbitastra.backend.repositories.finance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.Fee;
import com.orbitastra.backend.models.finance.enums.FeeStatus;

@Repository
public interface FeeRepository extends MongoRepository<Fee, String> {
    List<Fee> findBySchoolId(String schoolId);
    List<Fee> findByStudentId(String studentId);
    List<Fee> findBySchoolIdAndStudentId(String schoolId, String studentId);
    List<Fee> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<Fee> findByStudentIdAndAcademicYear(String studentId, String academicYear);
    List<Fee> findByStudentIdAndStatus(String studentId, FeeStatus status);
}
