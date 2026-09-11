package com.orbitastra.backend.repositories.academics.gradingscheme;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.grading.GradingScheme;

/**
 * Grading schemes, so a subject can say how its marks are read.
 *
 * <p><b>The last of the three this module's README listed as missing</b>, built 2026-09-11 with
 * #22 — {@code AffiliationProgrammeRepository} came with #12 and {@code StaffRepository} with
 * #17. Every reference the academic-structure endpoints accept can now be checked.
 *
 * <p>Without it, {@code gradingSchemeDocsId} would have been stored unvalidated: an id from
 * another school is a real id, and a subject marked against another school's scale is a wrong
 * report card rather than an error anybody would see.
 */
public interface GradingSchemeRepository extends MongoRepository<GradingScheme, String> {

    /**
     * One scheme, scoped to its school.
     *
     * <p>{@code schoolId} is in the query even though the id is globally unique — the tenant
     * boundary {@code SchoolBase} exists to enforce, and the reason a plain {@code findById}
     * would be a bug rather than a shortcut.
     */
    Optional<GradingScheme> findByIdAndSchoolId(String id, String schoolId);
}
