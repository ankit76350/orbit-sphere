package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
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

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

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

        //! step 3 - the code to store. Derived from the name, never accepted from the caller:
        //! six documents across three modules reference it, and a code that disagreed with the
        //! name it was made from would be a term nobody could find twice.
        String termCode = TextHelper.toCode(request.name(), 40);
        if (termCode.isEmpty()) {
            throw ApiException.conflict("TERM_CODE_INVALID",
                    "A term name must contain at least one letter or digit. Received: '"
                            + request.name() + "'.");
        }

        //! step 4 - the dates have to make sense on their own before they are compared to
        //! anything. An inverted range would otherwise be reported as "outside the year".
        helper.validateTermRange(request.startDate(), request.endDate());

        //! step 5 - and fall inside the year that will own them
        helper.validateTermWithinYear(request.startDate(), request.endDate(),
                year.getStartDate(), year.getEndDate());

        //! step 6 - ONE read of the year's terms. Every check below runs against it.
        // TODO: read academic terms
        List<AcademicTerm> yearTerms = academicTerms
                .findBySchoolIdAndAcademicYearOrderBySequenceAsc(school.getId(), year.getName());

        //! step 7 - the code and the sequence are unique in the year, retired terms included,
        //! because their indexes do not filter on active - so these checks must not either, or
        //! they would accept a write the index then refuses.
        //!
        //! THESE CHECKS ARE THE ENFORCEMENT, not a nicety in front of the index. The two unique
        //! indexes are declared on the model but built on demand (app.mongo.sync-indexes), and
        //! edusphere_dev carries neither - measured 2026-09-11, where academic_terms had only
        //! _id_. Where they ARE built they turn a duplicate-key 500 into this 409; where they
        //! are not, this is the only thing standing between a school and two TERM_1s.
        helper.validateTermCodeFree(yearTerms, null, termCode);
        helper.validateSequenceFree(yearTerms, null, request.sequence());

        //! step 8 - and the dates must not cover a day an ACTIVE term already covers
        helper.validateNoTermOverlap(yearTerms, null, request.startDate(), request.endDate());

        //! step 9 - a year weights every active term or none. The mixture is refused; a wrong
        //! total is only reported, at step 11 - see the helper for why the two differ.
        helper.validateWeightNotMixed(yearTerms, request.weightPercent());

        //! step 10 - insert. resultsLocked and active take their defaults: both are events with
        //! their own endpoints rather than fields to set at create.
        // TODO: create academic term
        AcademicTerm saved = academicTerms.save(AcademicTerm.builder()
                // Explicit, like every other write here. SchoolBase declares it @NotBlank, but
                // nothing validates a document on save — a term written without it is stored,
                // invisible to every tenant-scoped query, and only found by reading the raw
                // collection. Which is exactly how this was found.
                .schoolId(school.getId())
                .academicYear(year.getName())
                .termCode(termCode)
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
