package com.orbitastra.backend.repositories.crm.admissionoffer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionOffer;

/**
 * The part of #32 Spring Data cannot derive from a method name.
 *
 * <p>Five optional filters and a two-condition "expired" question do not fit a derived query, so
 * they are built by hand in the implementation beside this.
 */
public interface AdmissionOfferRepositoryCustom {

    /**
     * One page of offers.
     *
     * <p><b>{@code schoolId} is a parameter, never a field on the request.</b> The caller says what
     * to filter; the tenant is added by the service from the resolved school.
     */
    Page<AdmissionOffer> search(String schoolId, AdmissionOfferSearchRequest request,
            Pageable pageable);
}
