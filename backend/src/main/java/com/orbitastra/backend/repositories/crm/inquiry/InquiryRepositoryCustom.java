package com.orbitastra.backend.repositories.crm.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.inquiry.request.InquirySearchRequest;
import com.orbitastra.backend.models.crm.Inquiry;

/**
 * The part of #13 Spring Data cannot derive from a method name.
 *
 * <p>Five optional filters, a two-condition "overdue" question and a two-field search do not fit a
 * derived query, so they are built by hand in the implementation beside this.
 */
public interface InquiryRepositoryCustom {

    /**
     * One page of leads.
     *
     * <p><b>{@code schoolId} is a parameter, never a field on the request.</b> The caller says what
     * to filter; the tenant is added by the service from the resolved school.
     */
    Page<Inquiry> search(String schoolId, InquirySearchRequest request, Pageable pageable);
}
