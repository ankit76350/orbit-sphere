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
public interface GradingSchemeRepository
        extends MongoRepository<GradingScheme, String>, GradingSchemeRepositoryCustom {

    /**
     * One scheme, scoped to its school.
     *
     * <p>{@code schoolId} is in the query even though the id is globally unique — the tenant
     * boundary {@code SchoolBase} exists to enforce, and the reason a plain {@code findById}
     * would be a bug rather than a shortcut.
     */
    Optional<GradingScheme> findByIdAndSchoolId(String id, String schoolId);

    /**
     * Whether this school already has that version of that rulebook.
     *
     * <p><b>The pair is the key</b>, which {@code school_grading_name_version_uniq} declares — so
     * a school may hold "CBSE Percentage Grading" twice as long as the versions differ, and that
     * is the entire point of versioning a scheme rather than editing one.
     *
     * <p>The index named a {@code schemeCode} field that never existed until 2026-09-13, which
     * MongoDB indexed as null on every document: one version string per school, so two unrelated
     * rulebooks could not share a version number. Moving it to {@code name} is what makes this
     * query the one the index serves.
     */
    boolean existsBySchoolIdAndNameAndSchemeVersion(String schoolId, String name,
            String schemeVersion);

    /**
     * The same question, ignoring case on both halves of the key.
     *
     * <p><b>This is the one #1 and #3 actually ask, since 2026-09-16.</b> The exact-match version
     * above let a school store "CBSE" 2026.1 and "cbse" 2026.1 as two unrelated rulebooks — and
     * {@code name} is half the key rather than a label, so the two versions of one rulebook are
     * found by carrying the identical name. A casing difference therefore <b>splits one history
     * in two</b>, silently: exactly the harm the model's javadoc describes for a rename, arriving
     * through a route nothing was checking.
     *
     * <p><b>Stricter than the index, deliberately.</b> {@code school_grading_name_version_uniq}
     * is case-sensitive and stays that way — a case-insensitive index would need a collation and
     * a migration of every existing document. A check that refuses more than the index does is
     * safe; the reverse would be the bug. The same arrangement
     * {@code existsBySchoolIdAndDepartmentDocsIdAndTitleIgnoreCase} uses on a position title.
     *
     * <p><b>It cannot use the index</b>, which is the cost: an ignore-case derived query compiles
     * to a regex, so this is a collection scan within one school. A school holds tens of schemes,
     * and this runs once per write.
     */
    boolean existsBySchoolIdAndNameIgnoreCaseAndSchemeVersionIgnoreCase(String schoolId,
            String name, String schemeVersion);
}
