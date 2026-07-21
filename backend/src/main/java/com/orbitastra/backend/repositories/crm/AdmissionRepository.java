package com.orbitastra.backend.repositories.crm;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;

@Repository
public interface AdmissionRepository extends MongoRepository<Admission, String> {
    List<Admission> findBySchoolId(String schoolId);
    List<Admission> findBySchoolIdAndStatus(String schoolId, AdmissionStatus status);
    List<Admission> findByInquiryId(String inquiryId);
    boolean existsByInquiryId(String inquiryId);
}
