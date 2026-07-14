package com.orbitastra.backend.repositories.finance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.enums.FeeStatus;

@Repository
public interface FeeRepository extends MongoRepository<FeeInvoice, String> {
    List<FeeInvoice> findBySchoolId(String schoolId);
    List<FeeInvoice> findByStudentId(String studentId);
    List<FeeInvoice> findBySchoolIdAndStudentId(String schoolId, String studentId);
    List<FeeInvoice> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<FeeInvoice> findByStudentIdAndAcademicYear(String studentId, String academicYear);
    List<FeeInvoice> findByStudentIdAndStatus(String studentId, FeeStatus status);
}
