package com.orbitastra.backend.repositories.academics.schoolclass;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassSearchRequest;
import com.orbitastra.backend.models.academics.structure.SchoolClass;

/**
 * The part of {@link SchoolClassRepository} that cannot be a derived query method.
 *
 * <p>#28's filters are each optional, so no method name could express it: a name per combination,
 * and none of them matching a request that sends no filters at all. The same shape as
 * {@code SchoolSubscriptionRepositoryCustom}.
 *
 * <p><b>Spring Data finds the implementation by name and package</b> —
 * {@code SchoolClassRepositoryImpl}, beside this file. Renaming or moving it compiles, starts,
 * and fails only when somebody calls the method.
 */
public interface SchoolClassRepositoryCustom {

    /**
     * One year's classes, filtered, sorted and paged in the database.
     *
     * <p><b>{@code schoolId} and {@code academicYear} are separate from the request on purpose.</b>
     * They are the tenant boundary and the parent key rather than filters: both are always
     * applied, neither can be omitted, and neither is something a caller sends. Putting them in
     * the filter record would make them look optional.
     */
    Page<SchoolClass> search(String schoolId, String academicYear,
            SchoolClassSearchRequest request, Pageable pageable);
}
