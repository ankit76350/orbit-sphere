package com.orbitastra.backend.repositories.academics.academicterm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermSearchRequest;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;

/**
 * The one query a derived method name cannot express: five optional filters in any combination.
 *
 * <p>Spring Data would need a method per combination — thirty-two of them — so this is a fragment
 * with a {@link org.springframework.data.mongodb.core.MongoTemplate} behind it instead.
 */
public interface AcademicTermRepositoryCustom {

    /**
     * One page of a year's terms.
     *
     * @param schoolId     the tenant, never from the caller
     * @param academicYear the year in the path, never from the caller
     */
    Page<AcademicTerm> search(String schoolId, String academicYear,
            AcademicTermSearchRequest request, Pageable pageable);
}
