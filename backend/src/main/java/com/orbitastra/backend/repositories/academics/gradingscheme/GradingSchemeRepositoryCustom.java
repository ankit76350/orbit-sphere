package com.orbitastra.backend.repositories.academics.gradingscheme;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeSearchRequest;
import com.orbitastra.backend.models.academics.grading.GradingScheme;

/**
 * The part of {@link GradingSchemeRepository} a derived query name cannot express.
 *
 * <p><b>Three optional filters is why.</b> Spring Data derives a method name into one fixed query,
 * so "active, or scale type, or a name fragment, or any combination, or none" would be eight
 * method names — and adding a fourth filter would make it sixteen. One criteria builder instead.
 */
public interface GradingSchemeRepositoryCustom {

    /**
     * One page of a school's schemes, filtered and ordered in the database.
     *
     * @param schoolId the tenant boundary — never a filter the caller chose, and never optional
     */
    Page<GradingScheme> search(String schoolId, GradingSchemeSearchRequest request,
            Pageable pageable);
}
