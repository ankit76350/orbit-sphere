package com.orbitastra.backend.repositories.crm.admissionreview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionReview;

/**
 * The search #28 runs.
 *
 * <p>Every filter on it is optional, so the query has to be built at runtime rather than declared
 * as a method name — the same reason #5 and #24 have one of these.
 */
public interface AdmissionReviewRepositoryCustom {

    /**
     * One page of this school's reviews.
     *
     * <p><b>The school id is a parameter and never comes off the request.</b> There is no
     * {@code schoolId} on {@link AdmissionReviewSearchRequest} and there must never be one.
     */
    Page<AdmissionReview> search(String schoolId, AdmissionReviewSearchRequest request,
            Pageable pageable);
}
