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
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCreateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewSearchRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewUpdateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewResponse;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * How a school assesses an application. Endpoints #26, #27 and #28 of the plan in this package's
 * README; all three are built.
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
     * <p><b>And the rounds run 1, 2, 3 with no gaps.</b> Round 3 needs a round 2 to exist on the
     * application already, by anybody. A second assessor joining round 1 does not open round 2:
     * the rounds are the school's stages, not one person's.
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
     * 409 REVIEW_ROUND_OUT_OF_ORDER     asked for a round with nothing before it
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

    /**
     * Endpoint #27 — what the reviewer found.
     *
     * <p><b>Only what you send moves</b>, so a reviewer can save a score today and add the
     * recommendation tomorrow. A body that carries nothing is {@code 400 NOTHING_TO_UPDATE} rather
     * than a silent 200.
     *
     * <p><b>Moving it to {@code COMPLETED} is the completion</b>, and stamps {@code completedAt}.
     * There is no separate "finish" verb: the status is named directly, as #3 and #20 do. A
     * completed review <b>must</b> say what it recommends, and a cancelled one <b>must</b> say why.
     *
     * <p><b>Both ends are terminal.</b> A score typed wrong is corrected by cancelling this review
     * and assigning another — which leaves both in the history rather than overwriting one.
     *
     * <p><b>Addressed by its own id</b>, not under the application it belongs to. A reviewer opens
     * their own queue far more often than they walk down from a form.
     *
     * <pre>
     * 404 REVIEW_NOT_FOUND             no review with that id in this school
     * 409 REVIEW_ALREADY_COMPLETED     it is done, and a record is not a draft
     * 409 REVIEW_CANCELLED             the school called it off
     * 409 INVALID_REVIEW_TRANSITION    not a move it can make from where it is
     * 400 RECOMMENDATION_REQUIRED      completing without saying what is recommended
     * 400 CANCELLATION_NOTE_REQUIRED   cancelling without saying why
     * 400 NOTHING_TO_UPDATE            a body that changes nothing
     * 409 CONCURRENT_MODIFICATION      somebody recorded on it while you were reading
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PatchMapping("/reviews/{admissionReviewId}")
    public ResponseEntity<AdmissionReviewResponse> record(
            @PathVariable String admissionReviewId,
            @Valid @RequestBody AdmissionReviewUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The review's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionReviewService.recordResult(admissionReviewId, request));
    }

    /**
     * Endpoint #28 — a reviewer's queue. <b>What is due, and when.</b>
     *
     * <p>Filter by {@code reviewerDocsId} and {@code status} and you have one person's outstanding
     * work; those two plus {@code dueAt} are exactly {@code school_reviewer_status_due_idx}, which
     * exists for this. {@code overdue=true} is the sharper question — past its date <b>and</b>
     * still owed, because a review finished a month late also has a past due date.
     *
     * <p><b>Soonest due first.</b> A review with no due date sorts to the front, because Mongo puts
     * a missing field before every value — the {@code overdue} filter is what answers "what is
     * late" rather than the order.
     *
     * <p><b>A row is thinner than the review</b>: no criterion scores, no notes. Both can be large
     * and a page of twenty would carry all of it to draw a list that shows neither.
     *
     * <p><b>But the applicant is named</b>, in one query for the whole page. A queue of raw ids is
     * not a queue anybody can work from.
     *
     * <p><b>There is no "me".</b> Nothing in this project knows who is calling yet, so whose queue
     * it is has to be said out loud.
     *
     * <p><b>No gates.</b> A read — a suspended school still sees what it owes.
     *
     * <pre>
     * 400 INVALID_PAGE           a negative page
     * 400 INVALID_PAGE_SIZE      a size below 1 or above 100
     * 400 INVALID_SORT_FIELD     a field that is not on the allowlist
     * 400 TENANT_NOT_RESOLVED    no idtoken cookie
     * </pre>
     */
    @GetMapping("/reviews")
    public ResponseEntity<PageResponse<AdmissionReviewSummaryResponse>> list(
            AdmissionReviewSearchRequest request) {

        //! NO GATES. Reads run none: a school that cannot be edited still owes the reviews it
        //! handed out, and hiding them would make the backlog invisible exactly when it matters.
        return ResponseEntity.ok(admissionReviewService.listReviews(request));
    }
}
