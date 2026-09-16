package com.orbitastra.backend.services.academics.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;

import lombok.RequiredArgsConstructor;

/**
 * The reads {@link com.orbitastra.backend.services.academics.GradingSchemeService} makes more than
 * once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and a method here
 * never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class GradingSchemeServiceUtils {

    private final GradingSchemeRepository gradingSchemes;

    /**
     * One scheme, by its document id.
     *
     * <p><b>Scoped by {@code schoolId} even though a MongoDB id is globally unique.</b> That is the
     * tenant boundary {@code SchoolBase} exists to enforce, and the reason a plain
     * {@code findById} would be a bug rather than a shortcut: another school's id is a real id, and
     * a school reading another school's grade boundaries is a leak nothing else would catch.
     *
     * <p><b>There is no second key to scope by</b>, unlike a term — a scheme belongs to the school
     * rather than to one of its years, so there is no {@code {year}} in the path that a stale id
     * could contradict. Which also means this cannot 404 for "wrong year", only for "not yours".
     *
     * <p><b>An inactive scheme is returned</b>, deliberately. #7, #8 and #9 all have to answer for
     * a retired version: a report card issued in 2026 reprints through the 2026 rules long after
     * the school moved to 2027's. {@code active} governs what is offered for new work and nothing
     * else, so filtering it out here would break the one job a retired scheme still has.
     *
     * Used by:
     * - getScheme()
     * - setActive()
     * - updateScheme()
     */
    public GradingScheme loadScheme(School school, String schemeId) {
        String id = schemeId == null ? "" : schemeId.trim();

        // TODO: read grading scheme
        return gradingSchemes.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("GRADING_SCHEME_NOT_FOUND",
                        "No grading scheme with id '" + id + "' in this school."));
    }
}
