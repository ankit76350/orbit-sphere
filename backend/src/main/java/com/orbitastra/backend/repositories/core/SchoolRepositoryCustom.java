package com.orbitastra.backend.repositories.core;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.core.platform.SchoolSearchRequest;
import com.orbitastra.backend.models.core.School;

/**
 * The part of {@link SchoolRepository} that cannot be a derived query method.
 *
 * <p>G1 combines filters that are each optional, so there is no fixed method name that could
 * express it — {@code findByStatusInAndCountryCodeAndCity...} would be one method per
 * combination, and a caller sending no filters would match none of them.
 *
 * <p>Spring Data finds the implementation by name: {@code SchoolRepositoryImpl}. Renaming that
 * class breaks the wiring silently at startup, so do not.
 */
public interface SchoolRepositoryCustom {

    /** Filters, searches, sorts and pages, all in the database. */
    Page<School> search(SchoolSearchRequest request, Pageable pageable);
}
