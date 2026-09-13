package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradeBandRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeCreateRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradingSchemeResponse;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.services.academics.helper.GradingHelper;

import lombok.RequiredArgsConstructor;

/**
 * The school's grading rulebooks — the endpoints in
 * {@code controllers/academics/grading/README.md}. #1 is built.
 *
 * <p><b>Nothing here is scoped to an academic year</b>, unlike every other service in this module.
 * A rulebook outlives a year: the same scheme grades 2026-2027 and 2027-2028, and a report card
 * from either has to reprint identically years later. What moves when the rules move is
 * {@code schemeVersion}, not the calendar.
 *
 * <p><b>Which is also why no gate 4 runs above these.</b> Gate 4 asks whether a named year is the
 * school's working one, and there is no year in any of these paths to ask it about — so this is
 * the one academics surface that still answers after a school has ended its year. It has to:
 * correcting a 2026 report card means reading the 2026 scheme.
 */
@Service
@RequiredArgsConstructor
public class GradingSchemeService {

    private final GradingSchemeRepository gradingSchemes;
    private final GradingHelper helper;
    private final CurrentSchoolResolver currentSchool;

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    //! endpoint 1 — create a rulebook and its bands -----------------------------------

    /**
     * Endpoint #1 — create a scheme and its bands in one write.
     *
     * <p><b>The bands come with it rather than after it</b>, unlike the sections and subjects of a
     * class. A scheme with no bands grades nothing — it is not a partial scheme, it is not a
     * scheme — so there is no state worth saving between the two calls it would otherwise take.
     * The band set is also only ever valid as a whole, which is what rules out per-band endpoints
     * entirely.
     *
     * <p><b>The scale is read first, and everything else is checked against it.</b> A
     * {@code DESCRIPTOR} scheme refuses a ceiling and refuses bounds; {@code PERCENTAGE} and
     * {@code POINT} require all three. Asking about a band before knowing the scale gives the
     * right refusal for the wrong reason.
     *
     * <p><b>A gap is reported; an overlap is refused.</b> A gap means one mark has no grade —
     * visible, and fixable by whoever reads the warning. An overlap means one mark has two, and
     * which wins depends on the order the bands happen to be stored in.
     */
    @Transactional
    public GradingSchemeResponse createScheme(GradingSchemeCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the scale decides the shape of everything under it, so it is settled before
        //! a single band is looked at. DESCRIPTOR refuses a ceiling; the other two require one.
        helper.validateScaleCeiling(request.scaleType(), request.maximumValue());

        //! step 3 - and then the bands, in an order that matters. An inverted band checked last
        //! would be reported as an overlap - true, and the wrong thing to say about which band
        //! is wrong. Bounds first because the three checks below all read them.
        List<GradeBandRequest> bands = request.gradeBands();

        helper.validateBandBounds(request.scaleType(), bands);
        helper.validateBandRanges(bands);
        helper.validateBandCodesUnique(bands);
        helper.validateBandsWithinScale(request.scaleType(), request.maximumValue(), bands);
        helper.validateNoBandOverlap(request.scaleType(), bands);

        //! step 4 - the key is free. Checked AFTER the bands, deliberately: a caller who sent a
        //! broken scale should hear about the scale, not be told the name is taken and then have
        //! to fix the bands on the retry.
        //!
        //! THIS CHECK IS THE ENFORCEMENT, not a nicety in front of the index.
        //! school_grading_name_version_uniq is declared on the model but built on demand
        //! (app.mongo.sync-indexes), and it indexed a schemeCode field that never existed until
        //! 2026-09-13 - so any database created before then carries an index that constrains the
        //! wrong thing entirely.
        String name = request.name().trim();
        String version = request.schemeVersion().trim();

        // TODO: check grading scheme exists
        if (gradingSchemes.existsBySchoolIdAndNameAndSchemeVersion(school.getId(), name, version)) {
            throw ApiException.conflict("SCHEME_VERSION_TAKEN",
                    "This school already has '" + name + "' version " + version + ". A version "
                            + "number moves when the rules move — pick the next one, or read the "
                            + "existing scheme first.");
        }

        //! step 5 - build the bands IN THE ORDER GIVEN. Not re-sorted: a school listing A1 first
        //! means A1 first, and silently reordering makes a typo hard to spot against the paper it
        //! was copied from. passed defaults to true so an author lists the failures.
        List<GradeBand> stored = bands.stream()
                .map(band -> GradeBand.builder()
                        .gradeCode(band.gradeCode().trim())
                        .minimumValue(band.minimumValue())
                        .maximumValue(band.maximumValue())
                        .gradePoint(band.gradePoint())
                        .description(band.description() == null ? null : band.description().trim())
                        .passed(band.passed() == null || band.passed())
                        .build())
                .toList();

        //! step 6 - insert. active takes its default: it is an event with its own endpoints (#4,
        //! #5) rather than a field to set at create.
        // TODO: create grading scheme
        GradingScheme saved = gradingSchemes.save(GradingScheme.builder()
                // Explicit, like every other write in this module. SchoolBase declares it
                // @NotBlank, but nothing validates a document on save - one written without it
                // is stored, invisible to every tenant-scoped query, and found only by reading
                // the raw collection.
                .schoolId(school.getId())
                .name(name)
                .schemeVersion(version)
                .scaleType(request.scaleType())
                .maximumValue(request.maximumValue())
                .gradeBands(new ArrayList<>(stored))
                .build());

        //! step 7 - the gaps, REPORTED rather than refused. Computed against the request rather
        //! than the saved document because they are the same set and the request is already in
        //! hand - see the helper for why this is a warning at all.
        String warning = helper.gapWarning(request.scaleType(), request.maximumValue(), bands);

        return GradingSchemeResponse.fromScheme(saved, warning,
                "Reference this scheme by gradingSchemeDocsId — a subject, an exam and a report "
                        + "card all store it. Its name and version cannot be changed: moving a "
                        + "boundary means a new version (#2), because editing in place rewrites "
                        + "every report card ever issued. " + NO_AUTHORIZATION_YET);
    }
}
