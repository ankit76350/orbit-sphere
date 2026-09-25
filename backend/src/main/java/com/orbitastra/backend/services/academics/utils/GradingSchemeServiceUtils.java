package com.orbitastra.backend.services.academics.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradeBandRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradingSchemeResponse;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.services.academics.helper.GradingHelper;

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

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    public static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    private final GradingSchemeRepository gradingSchemes;
    private final CurrentSchoolResolver currentSchool;
    private final GradingHelper helper;

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

    /**
     * The one field #4 and #5 write, and the only thing that differs between them.
     *
     * <p>Two endpoints over one private method rather than one endpoint taking a boolean: the URL
     * is what says which way it goes, so a caller cannot half-read a body and retire a scheme it
     * meant to restore. The same arrangement the term lock pair uses.
     */
    public GradingSchemeResponse setActive(School school, GradingScheme scheme, boolean active) {

        //! step 2 - nothing to do if it is already in that state. A 200 rather than a 409: the
        //! caller asked for a state, not for a transition, and it is in that state.
        if (Boolean.valueOf(active).equals(scheme.getActive())) {
            return GradingSchemeResponse.fromScheme(scheme,
                    helper.gapWarning(scheme.getScaleType(), scheme.getMaximumValue(),
                            scheme.getGradeBands()),
                    "'" + scheme.getName() + "' version " + scheme.getSchemeVersion()
                            + " was already " + (active ? "active" : "retired")
                            + ". Nothing changed. " + NO_AUTHORIZATION_YET);
        }

        //! step 3 - active is the ONLY field this writes. Not the bands, not the key, not the
        //! scale - those are #3, and they are refused on a scheme anything references.
        scheme.setActive(active);

        // TODO: update grading scheme
        GradingScheme saved = gradingSchemes.save(scheme);

        //! step 4 - the gaps, recomputed from what is stored. #7 does the same, and for the same
        //! reason: nothing writes this onto the document, precisely so it cannot go stale.
        String warning = helper.gapWarning(
                saved.getScaleType(), saved.getMaximumValue(), saved.getGradeBands());

        return GradingSchemeResponse.fromScheme(saved, warning, active
                ? "'" + saved.getName() + "' version " + saved.getSchemeVersion()
                        + " is offered for new work again. " + NO_AUTHORIZATION_YET
                : "'" + saved.getName() + "' version " + saved.getSchemeVersion()
                        + " is no longer offered for new work. It still resolves every report "
                        + "card issued under it — retiring a scheme never changes a grade already "
                        + "printed. " + NO_AUTHORIZATION_YET);
    }

    /** One request band as it is stored. Shared by #1 and #3, so the two cannot drift. */
    public static GradeBand toBand(GradeBandRequest band) {
        return GradeBand.builder()
                .gradeCode(band.gradeCode().trim())
                .minimumValue(band.minimumValue())
                .maximumValue(band.maximumValue())
                .gradePoint(band.gradePoint())
                .description(band.description() == null ? null : band.description().trim())
                // Only false is ever sent; the model defaults it to true, and an author listing
                // eight bands should have to say which ones FAIL.
                .passed(band.passed() == null || band.passed())
                .build();
    }
}
