package com.orbitastra.backend.controllers.crm;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleCreateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionCycleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The rounds of admissions a school runs. Endpoints #1 to #7 of the plan in this package's README;
 * only #1 is built.
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
     * <p>Every <b>write</b> here runs gates 1 and 2. Reads will run none.
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
}
