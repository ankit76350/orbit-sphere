package com.orbitastra.backend.repositories.finance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.enums.FeeStatus;

@Repository
public interface FeeRepository extends MongoRepository<FeeInvoice, String> {
    List<FeeInvoice> findBySchoolId(String schoolId);
    List<FeeInvoice> findByStudentDocsId(String studentDocsId);
    List<FeeInvoice> findBySchoolIdAndStudentDocsId(String schoolId, String studentDocsId);
    List<FeeInvoice> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    List<FeeInvoice> findByStudentDocsIdAndAcademicYear(String studentDocsId, String academicYear);
    List<FeeInvoice> findByStudentDocsIdAndStatus(String studentDocsId, FeeStatus status);
}
