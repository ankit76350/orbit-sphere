package com.orbitastra.backend.repositories.crm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

@Repository
public interface InquiryRepository extends MongoRepository<Inquiry, String> {
    List<Inquiry> findBySchoolId(String schoolId);
    List<Inquiry> findBySchoolIdAndStatus(String schoolId, InquiryStatus status);
    List<Inquiry> findByCounselorId(String counselorId);
    List<Inquiry> findBySchoolIdAndNextFollowUpLessThanEqual(String schoolId, LocalDate date);
}
