package com.orbitastra.backend.controllers.crm;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCreateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * How a school assesses an application. Endpoints #26, #27 and #28 of the plan in this package's
 * README; #26 is built.
 *
 * <p><b>Its own controller, because {@code admission_reviews} is its own collection.</b> Five
 * collections get five controllers — the call this module's plan made after watching {@code people}
 * grow to fifteen endpoints across two documents in one file.
 *
 * <p><b>The base path is {@code /schools/current} and not a collection.</b> This controller's three
 * endpoints do not share one prefix: a review is <i>created</i> under the application it is of
 * ({@code POST /applications/{id}/reviews}), because a review has no meaning apart from that form
 * — but it is then <i>edited</i> and <i>listed</i> by its own id ({@code PATCH /reviews/{id}},
 * {@code GET /reviews}), because a reviewer opens their own queue far more often than they walk
 * down from an application. Splitting those across two controllers would put one collection's
 * writes in two files, which is the thing the five-controller rule exists to stop.
 *
 * <p><b>There is no {@code DELETE}.</b> A review assigned by mistake is cancelled — the
 * {@code CANCELLED} value on {@code AdmissionReviewStatus} is there for it — and #27 is what would
 * set it. Admissions keeps what it decided, including who it asked.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current")
public class AdmissionReviewController {

    private final AdmissionReviewService admissionReviewService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Writes run gates 1 and 2. <b>Gate 4 never runs in this module</b> — see the README. What
     * takes its place for a review is the <i>application's</i> status: a form nobody has submitted,
     * or one already decided, cannot be put on somebody's desk.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #26 — puts an application on a reviewer's desk.
     *
     * <p><b>It assigns the work; it does not do it.</b> The score, the criteria and the
     * recommendation are #27. Creating in {@code PENDING} is what makes "outstanding" a state that
     * exists, which is the whole of #28's queue.
     *
     * <p><b>A round can hold more than one reviewer</b> — an interview and an entrance test are two
     * reviews of round 1 — so the uniqueness rule is one reviewer per round, not one review per
     * round.
     *
     * <p><b>It moves the application to {@code UNDER_REVIEW}, and only from {@code SUBMITTED}.</b>
     * The second reviewer of a round moves nothing, and
     * {@code ADDITIONAL_INFORMATION_REQUIRED → UNDER_REVIEW} is #20's move rather than this one's.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND        no application with that id in this school
     * 409 APPLICATION_NOT_REVIEWABLE   a DRAFT nobody sent, or a form already decided
     * 404 STAFF_NOT_FOUND              a reviewer who is not this school's staff
     * 409 REVIEWER_ALREADY_ASSIGNED    that person already has that round of that form
     * 400 VALIDATION_FAILED            a missing reviewer or role, or a round outside 1 to 20
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/applications/{admissionApplicationId}/reviews")
    public ResponseEntity<AdmissionReviewResponse> assign(
            @PathVariable String admissionApplicationId,
            @Valid @RequestBody AdmissionReviewCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The application's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        AdmissionReviewResponse response =
                admissionReviewService.assignReviewer(admissionApplicationId, request);

        //! Addressed by its OWN id from here on, not by the application it came from — which is
        //! why this Location is /reviews/{id} rather than the path the request was made to.
        return ResponseEntity
                .created(URI.create("/schools/current/reviews/" + response.admissionReviewId()))
                .body(response);
    }
}
