package com.orbitastra.backend.services.academics.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
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
     */
    public AcademicYear loadAcademicYear(School school, String academicYear) {
        String year = academicYear == null ? "" : academicYear.trim();

        // TODO: read academic year
        return academicYears.findBySchoolIdAndName(school.getId(), year)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + year + "' in this school."));
    }
}
