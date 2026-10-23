package com.orbitastra.backend.repositories.crm.admissioncycle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionCycle;

/**
 * The searches that a derived query name cannot express.
 *
 * <p>Endpoint #5 filters by year and status, searches the name, and pages. A method name covering
 * all of that would be unreadable, and every filter is optional, so the query has to be built at
 * runtime rather than declared.
 */
public interface AdmissionCycleRepositoryCustom {

    /**
     * One page of this school's admission cycles.
     *
     * <p>The school id is passed in and never comes from the caller. It is the tenant boundary,
     * and a search that took it from the request would let one school read another's rounds.
     */
    Page<AdmissionCycle> search(String schoolId, AdmissionCycleSearchRequest request,
            Pageable pageable);
}
