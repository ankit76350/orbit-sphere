package com.orbitastra.backend.repositories.finance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.FeePayment;

@Repository
public interface FeePaymentRepository extends MongoRepository<FeePayment, String> {
    List<FeePayment> findByFeeDocsId(String feeDocsId);
    List<FeePayment> findByStudentDocsIdOrderByPaidOnDesc(String studentDocsId);
    List<FeePayment> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
}
