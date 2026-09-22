package com.orbitastra.backend.repositories.crm.admissionapplication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionApplication;

/** The search behind #24. Every filter is optional, so the query is built at runtime. */
public interface AdmissionApplicationRepositoryCustom {

    /**
     * One page of this school's applications.
     *
     * <p>The school id is passed in and never comes from the caller — it is the tenant boundary.
     */
    Page<AdmissionApplication> search(String schoolId, AdmissionApplicationSearchRequest request,
            Pageable pageable);
}
