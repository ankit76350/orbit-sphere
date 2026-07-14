package com.orbitastra.backend.repositories.finance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.FeePayment;

@Repository
public interface FeePaymentRepository extends MongoRepository<FeePayment, String> {
    List<FeePayment> findByFeeId(String feeId);
    List<FeePayment> findByStudentIdOrderByPaidOnDesc(String studentId);
    List<FeePayment> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
}
