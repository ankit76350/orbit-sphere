package com.orbitastra.backend.controllers.crm;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationCreateRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The forms families fill in. Endpoints #17 to #25 and #33 of the plan in this package's README;
 * only #17 is built.
 *
 * <p><b>Its own controller, not part of the cycle's.</b> Five collections get five controllers —
 * the call this module's plan made after watching {@code people} grow to fifteen endpoints across
 * two documents in one file.
 *
 * <p><b>An application is addressed by its own id</b>, not nested under the cycle. An admission
 * officer opens one from a worklist or a search far more often than by walking down from a cycle,
 * and a nested address would make the common call carry an id nobody had.
 *
 * <p><b>There is no {@code DELETE}.</b> An application the family pulled out of is {@code WITHDRAWN}
 * — #21. Admissions is the record of what a school decided about a child, and the decision not to
 * go ahead is part of it.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/applications")
public class AdmissionApplicationController {

    private final AdmissionApplicationService admissionApplicationService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Writes run gates 1 and 2. <b>Gate 4 never runs in this module</b> — a cycle for a year
     * that has not started is the normal case, so asking "is this the running year" would refuse
     * the work the module exists to do. What takes its place is the cycle's own status: an
     * application can only go into an {@code OPEN} cycle.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #17 — starts an application against an open cycle.
     *
     * <p><b>The inquiry is optional</b>, and that is the point: the family that walks in with a
     * completed form never enquired. It is also why the whole pipeline is testable without a
     * single lead in the database.
     *
     * <p><b>The class must be in the cycle's seat table</b>, not just in its year. A class with no
     * seats is a class nothing could ever be offered in.
     *
     * <p>Creates in {@code DRAFT}. Submitting is #19, which is not built.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND   no cycle with that id in this school
     * 409 CYCLE_NOT_OPEN              the cycle is not taking applications
     * 404 INQUIRY_NOT_FOUND           an inquiry that is not this school's
     * 409 APPLICATION_ALREADY_EXISTS  that inquiry already applied to that cycle
     * 409 CLASS_NOT_IN_CYCLE_YEAR     the class is not of the cycle's year
     * 409 CLASS_NOT_IN_CAPACITY       the cycle's seat table does not list that class
     * 400 TOO_MANY_FORM_ANSWERS       more answers than the cap
     * 400 VALIDATION_FAILED           a missing cycle, class, name, date of birth, gender or guardian
     * </pre>
     */
    @PostMapping
    public ResponseEntity<AdmissionApplicationResponse> create(
            @Valid @RequestBody AdmissionApplicationCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The cycle's own status is what decides, and the service asks it.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        AdmissionApplicationResponse response =
                admissionApplicationService.createApplication(request);

        return ResponseEntity
                .created(URI.create("/schools/current/applications/"
                        + response.admissionApplicationId()))
                .body(response);
    }
}
