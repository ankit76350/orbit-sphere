package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradeBandRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeCreateRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeSearchRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradingSchemeResponse;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradingSchemeSummary;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.services.academics.helper.GradingHelper;
import com.orbitastra.backend.services.academics.utils.GradingSchemeServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The school's grading rulebooks — the endpoints in
 * {@code controllers/academics/grading/README.md}. #1, #6 and #7 are built.
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
    private final GradingSchemeServiceUtils utils;
    private final GradingHelper helper;
    private final CurrentSchoolResolver currentSchool;

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * What #6 may be sorted by, lowercase key to real field.
     *
     * <p>An <b>allowlist</b>, not a passthrough: an arbitrary field name reaching a Mongo sort is
     * how a caller makes the database read every document in the collection to answer one page.
     *
     * <p>{@code gradeBands} is deliberately absent. Sorting by an array sorts by its first element
     * in Mongo, which would order schemes by whichever band happened to be entered first — a
     * result that looks deliberate and means nothing.
     */
    private static final Map<String, String> SORTABLE_SCHEME_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_SCHEME_FIELDS.put("name", "name");
        SORTABLE_SCHEME_FIELDS.put("schemeversion", "schemeVersion");
        SORTABLE_SCHEME_FIELDS.put("scaletype", "scaleType");
        SORTABLE_SCHEME_FIELDS.put("createdat", "createdAt");
        SORTABLE_SCHEME_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_SCHEME_FIELD_NAMES =
            SORTABLE_SCHEME_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order: the rulebook, then its versions oldest-looking first.
     *
     * <p><b>It is also the tiebreaker on every other sort</b>, and that is not this class's doing:
     * {@link PageResponse#pageableOf} appends the fallback to whatever the caller named, minus any
     * key they already used. So {@code ?sort=scaleType} is really
     * {@code scaleType, name, schemeVersion}.
     *
     * <p><b>Which makes the choice of fallback the decision that matters.</b> The pair is unique
     * within a school — {@code school_grading_name_version_uniq} declares it and #1 enforces it —
     * so every sort ends in a total order and paging cannot put one row on two pages while another
     * is never seen. <b>Neither field alone would have served</b>: a school holds one name at
     * several versions, and one version string across several names. This is the first list in the
     * project whose stable order needs two fields rather than one.
     *
     * <p>Grouping the versions of one rulebook together is the useful side effect, not the reason.
     */
    private static final Sort SCHEME_ORDER =
            Sort.by(Sort.Order.asc("name"), Sort.Order.asc("schemeVersion"));

 

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
     * {@code MARKS} require all three. Asking about a band before knowing the scale gives the
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

        //! step 7 - the gaps, REPORTED rather than refused. Computed from the SAVED document
        //! rather than the request: the two are the same set, and taking the saved one means #7
        //! recomputes it from exactly the same input on every later read - see the helper.
        String warning = helper.gapWarning(
                saved.getScaleType(), saved.getMaximumValue(), saved.getGradeBands());

        return GradingSchemeResponse.fromScheme(saved, warning,
                "Reference this scheme by gradingSchemeDocsId — a subject, an exam and a report "
                        + "card all store it. Its name and version cannot be changed: moving a "
                        + "boundary means a new version (#2), because editing in place rewrites "
                        + "every report card ever issued. " + NO_AUTHORIZATION_YET);
    }


    //! endpoint 6 — the school's schemes ----------------------------------------------

    /**
     * Endpoint #6 — one page of the school's schemes, filtered.
     *
     * <p><b>The dropdown behind every "how is this graded" field.</b> Which is why it filters by
     * {@code scaleType}: a caller attaching a scheme to an exam marked out of 100 wants the
     * schemes that can resolve a number, and {@code DESCRIPTOR} cannot.
     *
     * <p><b>Bands are not returned, only {@code bandCount}.</b> A twelve-row page carrying twelve
     * full band tables is roughly six hundred values to render a list of twelve names. #7 is one
     * call away for the caller that wants them.
     *
     * <p><b>No gates, and {@code require} rather than {@code requireUsable}.</b> Looking at a
     * rulebook is not an action on it, and a school that has stopped paying still has to be able
     * to read the rules its old report cards were issued under.
     *
     * <p><b>Nothing here can 404.</b> Unlike the term list, which resolves a {@code {year}} from
     * the path and answers 404 when it is not this school's, there is no parent to resolve — so an
     * empty page means "this school has no schemes", which is a fact rather than an ambiguity.
     */
    public PageResponse<GradingSchemeSummary> listSchemes(GradingSchemeSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_SCHEME_FIELDS, SORTABLE_SCHEME_FIELD_NAMES, SCHEME_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own grading rules.
        School school = currentSchool.require();

        //! step 3 - one page, filtered and ordered in the database. The tenant is passed
        //! separately from the request because it is the boundary, not a filter.
        // TODO: read grading schemes
        return PageResponse.from(
                gradingSchemes.search(school.getId(), request, pageable),
                // The summary, not the full response: no bands, and no warning on a row that
                // could not act on one.
                GradingSchemeSummary::fromScheme);
    }

    //! endpoint 7 — one scheme, with its bands ---------------------------------------

    /**
     * Endpoint #7 — one scheme with every band, in stored order.
     *
     * <p><b>What a school reads to check its own boundaries</b>, and the only endpoint that returns
     * the bands at all — #6 trims them to a count, because a page of full band tables is hundreds
     * of values nobody reads.
     *
     * <p><b>Bands come back in the order they were written</b>, never re-sorted. A school listing
     * A1 first means A1 first, and a response that silently reordered them would make a typo hard
     * to spot against the paper they were copied from. The checks in #1 sort a copy, because
     * overlap is a question about the set rather than about the list.
     *
     * <p><b>The gap warning is recomputed here, not stored.</b> A school that ignored it on create
     * should still see it every time it looks — and a stored sentence would outlive the problem it
     * described, so a scheme whose bands were fixed by #3 would keep being warned about a hole
     * that is no longer there.
     *
     * <p><b>It answers for a retired scheme.</b> {@code active} governs what is offered for new
     * work and nothing else: a report card issued in 2026 has to reprint through the 2026 rules
     * long after the school moved to 2027's.
     *
     * <p><b>No gates, and {@code require} rather than {@code requireUsable}.</b> Looking at a
     * rulebook is not an action on it.
     */
    public GradingSchemeResponse getScheme(String schemeId) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read the rules its old report cards were issued under.
        School school = currentSchool.require();

        //! step 2 - the scheme, scoped to the school. Retired ones answer too.
        GradingScheme scheme = utils.loadScheme(school, schemeId);

        //! step 3 - the gaps, RECOMPUTED against what is stored. Never read from the document:
        //! nothing writes it there, precisely so it cannot go stale.
        String warning = helper.gapWarning(
                scheme.getScaleType(), scheme.getMaximumValue(), scheme.getGradeBands());

        return GradingSchemeResponse.fromScheme(scheme, warning,
                "Bands are in the order they were written, never re-sorted. This scheme's name and "
                        + "version cannot be changed: moving a boundary means a new version (#2), "
                        + "because editing in place rewrites every report card ever issued. "
                        + NO_AUTHORIZATION_YET);
    }

}
