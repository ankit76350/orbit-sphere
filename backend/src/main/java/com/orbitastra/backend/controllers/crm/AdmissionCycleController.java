package com.orbitastra.backend.controllers.crm;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleCreateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleSearchRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleUpdateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleDetailResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionCycleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The rounds of admissions a school runs. Endpoints #1 to #7 of the plan in this package's README;
 * #1, #2, #5 and #6 are built.
 *
 * <p>School surface, so the school comes from CurrentSchoolResolver and never from the URL.
 *
 * <p><b>There is no {@code {year}} in the path</b>, which is the opposite of what the classes and
 * timetable routes do. For those the year is the scope — every class belongs to the year the school
 * is running. An admission cycle belongs to a year the school has <b>not</b> started yet, and a
 * school often has two cycles live at once: late admissions into this year while next year's cycle
 * is open. Putting the year in the path would claim a scope this module does not have, and would
 * make "show me both" impossible to ask for. So the year is a normal field on the cycle.
 *
 * <p><b>There is no {@code DELETE}.</b> A cycle that came to nothing is CANCELLED through #3.
 * Admissions is the record of what a school decided, and the rounds it abandoned are part of that.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/admission-cycles")
public class AdmissionCycleController {

    private final AdmissionCycleService admissionCycleService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Every <b>write</b> here runs gates 1 and 2. <b>Reads run none</b> — a suspended school
     * still sees the rounds it ran, because the children it admitted are still admitted.
     *
     * <p><b>Gate 4 does not run on anything in this module, and that is deliberate.</b> Everywhere
     * else, writing against a year the school is not running is a mistake. Here it is the normal
     * case — a 2027-2028 cycle is created, opened, filled and enrolled from entirely inside
     * 2026-2027 — so calling {@code requireYearMarkedAsRunning} would refuse the work this module
     * exists to do. What takes its place is the cycle's own status: applications can only be taken
     * into a cycle that is OPEN. That is a check on the cycle, not on the year.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — sets up a new admission cycle.
     *
     * <p><b>The first call anybody makes in this module.</b> Every application has to name a cycle,
     * so nothing else here works until one exists.
     *
     * <p>The cycle is created as a DRAFT with no seats. Seats are set by #4 and the cycle is opened
     * by #3 — a school names and dates a round before it has worked out how many seats each class
     * gets.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND   no year with that name in this school
     * 409 CYCLE_NAME_TAKEN          that year already has a cycle with that name
     * 400 CYCLE_DATES_OUT_OF_ORDER  the dates given are not in a sensible order
     * </pre>
     */
    @PostMapping
    public ResponseEntity<AdmissionCycleResponse> create(
            @Valid @RequestBody AdmissionCycleCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. See the note on the fields above; admissions are set up for a
        //! year that has not started, so asking "is this the running year" refuses the
        //! normal case.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        AdmissionCycleResponse response = admissionCycleService.createCycle(request);
        return ResponseEntity
                .created(URI.create("/schools/current/admission-cycles/"
                        + response.admissionCycleId()))
                .body(response);
    }

    /**
     * Endpoint #5 — one page of this school's admission cycles.
     *
     * <p>Filter by {@code academicYear} and {@code status}, search the name with {@code search},
     * and ask which rounds were taking applications on a day with {@code openOn}. Every one is
     * optional; sending none returns the school's cycles, newest year first.
     *
     * <p><b>No year in the path</b>, and none is required as a filter either. A school works on
     * two years at once during admissions — late admissions into the running year while next
     * year's round is open — so "show me both" is the default rather than something to ask for.
     *
     * <p><b>No gates.</b> This is a read.
     *
     * <pre>
     * 400 INVALID_PAGE          a negative page
     * 400 INVALID_PAGE_SIZE     a size below 1 or above 100
     * 400 INVALID_SORT_FIELD    a field that is not in the allowlist
     * 400 INVALID_SORT_DIRECTION  a direction that is not asc or desc
     * 400 TENANT_NOT_RESOLVED   no idtoken cookie
     * 404 SCHOOL_NOT_FOUND      the cookie names a school that is not there
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdmissionCycleSummaryResponse>> list(
            AdmissionCycleSearchRequest request) {

        //! NO GATES. Reads run none: gate 1 would stop a suspended school reading the admissions
        //! it already ran, and gate 2 would make a lapsed subscription hide the school's own
        //! history rather than stop it changing anything.
        return ResponseEntity.ok(admissionCycleService.listCycles(request));
    }

    /**
     * Endpoint #6 — one cycle in full, with its seat table.
     *
     * <p>What this adds over a row of [#5]: the {@code notes}, and the seat table itself rather
     * than a count of it. Each seat row carries its class's <b>name</b>, because a table of raw
     * document ids is not readable.
     *
     * <p><b>It does not say how the seats are doing.</b> Offered, accepted, enrolled and free are
     * counted from the applications and belong to #7, which is not built.
     *
     * <p><b>The cycle is addressed by its document id</b> — what applications will store as
     * {@code admissionCycleDocsId}. No {@code {year}} in front of it: a cycle's year is a property
     * of the cycle, and an id is unique without one.
     *
     * <p><b>No gates.</b> This is a read.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND  no cycle with that id in THIS school
     * 400 TENANT_NOT_RESOLVED        no idtoken cookie
     * </pre>
     */
    @GetMapping("/{admissionCycleId}")
    public ResponseEntity<AdmissionCycleDetailResponse> get(
            @PathVariable String admissionCycleId) {

        //! NO GATES. A read, so a suspended school still opens the rounds it ran.
        return ResponseEntity.ok(admissionCycleService.getCycle(admissionCycleId));
    }

    /**
     * Endpoint #2 — corrects a cycle's name, dates or notes.
     *
     * <p><b>Only what is sent moves.</b> An absent field is left alone. A field named in
     * {@code clear} is emptied — which the four dates need, because there is no such thing as an
     * empty instant and a record cannot tell an absent key from a null one.
     *
     * <p><b>The dates are checked as they will end up</b>, merged with what is already stored.
     * Sending a close date that is fine on its own and wrong against the stored open date is the
     * case this endpoint exists to catch.
     *
     * <p><b>What cannot be changed here.</b> {@code academicYear} — that is a different cycle, not
     * a correction. {@code status} — #3, whose moves have preconditions a field edit cannot carry.
     * {@code capacities} — #4, which replaces the seat table whole.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND   no cycle with that id in this school
     * 400 NOTHING_TO_UPDATE           the body moves nothing
     * 400 BLANK_CYCLE_NAME            name sent as "" — a cycle needs one
     * 400 UNKNOWN_CLEAR_FIELD         clear names something that is not clearable
     * 400 CLEAR_CONFLICTS_WITH_VALUE  a field both cleared and given a value
     * 409 CYCLE_NAME_TAKEN            that year already has a cycle of that name
     * 400 CYCLE_DATES_OUT_OF_ORDER    the result would not run forwards
     * 409 CONCURRENT_MODIFICATION     a version was sent and the cycle has moved on
     * </pre>
     */
    @PatchMapping("/{admissionCycleId}")
    public ResponseEntity<AdmissionCycleResponse> update(
            @PathVariable String admissionCycleId,
            @Valid @RequestBody AdmissionCycleUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN, the same as #1. A cycle for a year that has not started is the
        //! normal thing to be correcting.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(admissionCycleService.updateCycle(admissionCycleId, request));
    }
}
