package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermSearchRequest;
import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermCreateRequest;
import com.orbitastra.backend.dto.academics.academicterm.response.AcademicTermResponse;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.academicterm.AcademicTermRepository;
import com.orbitastra.backend.services.academics.helper.AcademicsHelper;
import com.orbitastra.backend.services.academics.utils.AcademicTermServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The reporting periods of an academic year — endpoints #1 to #11 of the plan in
 * {@code controllers/academics/structure/README.md}. #1 is built.
 *
 * <p><b>A term is a document, not an embedded row</b>, unlike a section or a subject. Six
 * documents across three modules store {@code termDocsId}, so a term needs an id — which is also
 * why it can be renamed safely and why a section can never be.
 *
 * <p><b>The gates run in the controller</b>, not here.
 */
@Service
@RequiredArgsConstructor
public class AcademicTermService {

    private final CurrentSchoolResolver currentSchool;
    private final AcademicTermRepository academicTerms;
    private final AcademicTermServiceUtils utils;
    private final AcademicsHelper helper;

    /**
     * What {@code ?sort=} accepts, keyed by the lower-cased name a caller types.
     *
     * <p>An allowlist rather than a pass-through: an arbitrary field name reaching a Mongo sort is
     * how a caller sorts on something unindexed and makes the database read every row of the
     * collection to answer.
     */
    private static final Map<String, String> SORTABLE_TERM_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_TERM_FIELDS.put("sequence", "sequence");
        SORTABLE_TERM_FIELDS.put("name", "name");
        SORTABLE_TERM_FIELDS.put("startdate", "startDate");
        SORTABLE_TERM_FIELDS.put("enddate", "endDate");
        SORTABLE_TERM_FIELDS.put("createdat", "createdAt");
        SORTABLE_TERM_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_TERM_FIELD_NAMES =
            SORTABLE_TERM_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the one the plan asks for: a year's terms in the order they happen.
     *
     * <p><b>It is also the tiebreaker on every other sort</b>, and that is not this class's doing:
     * {@link PageResponse#pageableOf} appends the fallback order to whatever the caller named,
     * minus any key they already used. So {@code ?sort=name} is really {@code name, sequence}.
     *
     * <p><b>Which makes the choice of fallback the decision that matters here.</b> {@code sequence}
     * is unique within a year — #1 enforces it and {@code school_year_term_sequence_uniq} declares
     * it — so every sort ends in a total order and paging cannot put one row on two pages. A term
     * {@code name} is <i>not</i> unique, so it could not have served.
     *
     * <p>A private {@code withStableOrder} was written here first and deleted on 2026-09-11: it
     * appended {@code sequence} by hand and was entirely redundant, which a mutation removing it
     * proved by changing nothing at all.
     */
    private static final Sort TERM_ORDER = Sort.by(Sort.Order.asc("sequence"));

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * Endpoint #9 — one page of the year's terms.
     *
     * <p><b>Paged, though the plan said not to.</b> The plan's reasoning was that a year holds two
     * to four terms and a page cursor on a four-row list is machinery nobody uses. Nothing
     * enforces that, though: a school running monthly reporting periods has twelve, and the cost
     * of paging here is one shared record and one shared factory that already exist. A client
     * that handles every list in this API the same way is worth more than the four rows saved.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own calendar.
     */
    public PageResponse<AcademicTermResponse> listTerms(String academicYear,
            AcademicTermSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_TERM_FIELDS, SORTABLE_TERM_FIELD_NAMES, TERM_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own structure.
        School school = currentSchool.require();

        //! step 3 - the year in the path has to be one this school actually has. No gate runs on
        //! a read, so this is what answers 404 rather than handing back an empty page for a year
        //! that does not exist - which would read as "this year has no terms".
        AcademicYear year = utils.loadAcademicYear(school, academicYear);

        //! step 4 - one page, filtered and ordered in the database. The tenant and the year are
        //! passed separately from the request because they are the boundary, not filters.
        // TODO: read academic terms
        return PageResponse.from(
                academicTerms.search(school.getId(), year.getName(), request, pageable),
                // The single-argument factory, so no `warning` or `nextStep` appears on a row: a
                // read changed nothing, and a null on every row is noise a client has to decide
                // whether to trust.
                AcademicTermResponse::fromTerm);
    }

    /**
     * Endpoint #1 — add one reporting period to the year.
     *
     * <p><b>Five rules MongoDB cannot express, all answered from one read.</b> The year's terms
     * are loaded once and every check runs against that snapshot, so they cannot disagree with
     * each other — a year holds two to four terms, so the set costs nothing to hold.
     *
     * <p><b>Two of those rules count retired terms and one does not.</b> A retired term keeps its
     * code and its sequence, because neither unique index filters on {@code active}; it releases
     * its dates, because nothing is taught in it. The helper says so at each check.
     */
    public AcademicTermResponse createTerm(String academicYear,
            AcademicTermCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the year, as a document: a term has to fall inside its dates, so existence
        //! alone is not enough here.
        AcademicYear year = utils.loadAcademicYear(school, academicYear);

        //! step 3 - the dates have to make sense on their own before they are compared to
        //! anything. An inverted range would otherwise be reported as "outside the year".
        helper.validateTermRange(request.startDate(), request.endDate());

        //! step 4 - and fall inside the year that will own them
        helper.validateTermWithinYear(request.startDate(), request.endDate(),
                year.getStartDate(), year.getEndDate());

        //! step 5 - ONE read of the year's terms. Every check below runs against it.
        // TODO: read academic terms
        List<AcademicTerm> yearTerms = academicTerms
                .findBySchoolIdAndAcademicYearOrderBySequenceAsc(school.getId(), year.getName());

        //! step 6 - the code and the sequence are unique in the year, retired terms included,
        //! because their indexes do not filter on active - so these checks must not either, or
        //! they would accept a write the index then refuses.
        //!
        //! THESE CHECKS ARE THE ENFORCEMENT, not a nicety in front of the index. The two unique
        //! indexes are declared on the model but built on demand (app.mongo.sync-indexes), and
        //! edusphere_dev carries neither - measured 2026-09-11, where academic_terms had only
        //! _id_. Where they ARE built they turn a duplicate-key 500 into this 409; where they
        //! are not, this is the only thing standing between a school and two TERM1s.
        helper.validateTermCodeFree(yearTerms, null, request.termCode());
        helper.validateSequenceFree(yearTerms, null, request.sequence());

        //! step 7 - and the dates must not cover a day an ACTIVE term already covers
        helper.validateNoTermOverlap(yearTerms, null, request.startDate(), request.endDate());

        //! step 8 - a year weights every active term or none. The mixture is refused; a wrong
        //! total is only reported, at step 10 - see the helper for why the two differ.
        helper.validateWeightNotMixed(yearTerms, request.weightPercent());

        //! step 9 - insert. resultsLocked and active take their defaults: both are events with
        //! their own endpoints rather than fields to set at create.
        // TODO: create academic term
        AcademicTerm saved = academicTerms.save(AcademicTerm.builder()
                // Explicit, like every other write here. SchoolBase declares it @NotBlank, but
                // nothing validates a document on save — a term written without it is stored,
                // invisible to every tenant-scoped query, and only found by reading the raw
                // collection. Which is exactly how this was found.
                .schoolId(school.getId())
                .academicYear(year.getName())
                .termCode(request.termCode())
                .name(request.name().trim())
                .sequence(request.sequence())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .weightPercent(request.weightPercent())
                .build());

        //! step 11 - the weight sum, reported rather than refused, against the set INCLUDING the
        //! row just written - which is the total a school would see on screen.
        List<AcademicTerm> after = new ArrayList<>(yearTerms);
        after.add(saved);

        return AcademicTermResponse.fromTerm(saved, helper.weightSumWarning(after),
                "Exams and report cards reference this term by termDocsId. " + NO_AUTHORIZATION_YET);
    }
}
