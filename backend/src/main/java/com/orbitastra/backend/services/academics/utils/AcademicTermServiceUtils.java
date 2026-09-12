package com.orbitastra.backend.services.academics.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.academicterm.AcademicTermRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * The reads {@link com.orbitastra.backend.services.academics.AcademicTermService} makes more than
 * once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and a method here
 * never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class AcademicTermServiceUtils {

    private final AcademicYearRepository academicYears;
    private final AcademicTermRepository academicTerms;

    /**
     * The academic year named in the path, as a document rather than a name.
     *
     * <p><b>Terms need the year's dates, not just its existence</b> — which is what separates this
     * from {@code SchoolClassServiceUtils.requireAcademicYear}. A class only has to belong to a
     * year that exists; a term has to fall inside one, so the containment check needs
     * {@code startDate} and {@code endDate} and there is no second query that would supply them.
     *
     * <p>Scoped by {@code schoolId}, never by name alone: two schools may both run a "2026-2027".
     *
     * Used by:
     * - createTerm()
     * - updateTerm()
     */
    public AcademicYear loadAcademicYear(School school, String academicYear) {
        String year = academicYear == null ? "" : academicYear.trim();

        // TODO: read academic year
        return academicYears.findBySchoolIdAndName(school.getId(), year)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + year + "' in this school."));
    }

    /**
     * One term of one year, by its document id.
     *
     * <p><b>A term is addressed by its id, not by its code</b>, which the module README settled on
     * 2026-09-10: the rule is not "prefer codes" but "use whatever other collections already
     * store", and six documents across three modules store {@code termDocsId}. That is also what
     * keeps {@code name} editable — nothing joins on it.
     *
     * <p>Scoped by {@code schoolId} <i>and</i> by the year in the URL. The id alone would find the
     * right document, but it would also find one belonging to another year of the same school, and
     * an endpoint reached through {@code /academic-years/{year}/terms/{termId}} that edits a term
     * of a different year is editing something nobody asked about.
     *
     * Used by:
     * - updateTerm()
     */
    public AcademicTerm loadTerm(School school, String academicYear, String termId) {
        String id = termId == null ? "" : termId.trim();

        // TODO: read academic term
        return academicTerms
                .findByIdAndSchoolIdAndAcademicYear(id, school.getId(), academicYear)
                .orElseThrow(() -> ApiException.notFound("TERM_NOT_FOUND",
                        "No term with id '" + id + "' in '" + academicYear + "'."));
    }
}
