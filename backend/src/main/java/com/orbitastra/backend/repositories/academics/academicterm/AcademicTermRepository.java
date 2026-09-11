package com.orbitastra.backend.repositories.academics.academicterm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.structure.AcademicTerm;

/**
 * The reporting periods of one academic year.
 *
 * <p><b>Every query carries {@code schoolId}.</b> A term is a {@code SchoolBase} document and
 * {@code termCode} is unique only within one school's year, so a lookup by code alone would find
 * another school's term — the bug the tenant-scoped lookups elsewhere in this module exist to
 * prevent.
 *
 * <p><b>The year is its name, not an id.</b> {@code academicYear} stores
 * {@code AcademicYear.name}, so these take the same string the URL carries.
 */
public interface AcademicTermRepository
        extends MongoRepository<AcademicTerm, String>, AcademicTermRepositoryCustom {

    /**
     * Every term of one year, in sequence order.
     *
     * <p><b>One query serves all of #1's checks.</b> A year holds two to four terms, so loading
     * the set costs nothing and lets code uniqueness, sequence uniqueness, overlap and the weight
     * rules be answered without four round trips — and answered against the same snapshot, so
     * they cannot disagree with each other.
     */
    List<AcademicTerm> findBySchoolIdAndAcademicYearOrderBySequenceAsc(String schoolId,
            String academicYear);

    /** One term by its code, scoped to the school and the year. */
    Optional<AcademicTerm> findBySchoolIdAndAcademicYearAndTermCode(String schoolId,
            String academicYear, String termCode);
}
