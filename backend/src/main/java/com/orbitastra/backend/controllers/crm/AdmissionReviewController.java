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
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCancelRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewRecommendationRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCompleteRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCreateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewSearchRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewAddReviewRequest;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewResponse;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * How a school assesses an application. Endpoints #26, #27, #28 of the plan in this package's
 * README, and the three verbs the plan did not have — #27b, #27c and #27d. All six are built.
 *
 * <p><b>Its own controller, because {@code admission_reviews} is its own collection.</b> Five
 * collections get five controllers — the call this module's plan made after watching {@code people}
 * grow to fifteen endpoints across two documents in one file.
 *
 * <p><b>THE THREE VERBS ARE NOT A SECOND WAY TO DO #27.</b> {@code PATCH /reviews/{id}} can set any
 * of these statuses, and a reviewer saving a score halfway through still wants it. But starting,
 * finishing and calling off are <i>events</i> rather than fields being set, and this module's rule —
 * the one #19 and #3 follow — is that events get a verb. A verb can also ask for what its move needs
 * and nothing else: {@code /complete} takes a recommendation, {@code /cancel} takes a reason, and
 * neither can be sent an empty body that means nothing.
 *
 * <p><b>The base path is {@code /schools/current} and not a collection.</b> This controller's
 * endpoints do not share one prefix: a review is <i>created</i> under the application it is of
 * ({@code POST /applications/{id}/reviews}), because a review has no meaning apart from that form
 * — but it is then <i>edited</i> and <i>listed</i> by its own id ({@code PATCH /reviews/{id}},
 * {@code GET /reviews}), because a reviewer opens their own queue far more often than they walk
 * down from an application. Splitting those across two controllers would put one collection's
 * writes in two files, which is the thing the five-controller rule exists to stop.
 *
 * <p><b>There is no {@code DELETE}.</b> A review assigned by mistake is cancelled — #27d is that
 * call, and the {@code CANCELLED} value on {@code AdmissionReviewStatus} is there for it.
 * Admissions keeps what it decided, including who it asked and that it changed its mind.
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
     * <p><b>The round is COUNTED, not sent — changed 2026-09-30.</b> It is the number of reviews
     * the form already has, plus one, so rounds run 1, 2, 3 with no gaps and no duplicates because
     * nothing is left to type wrong. It used to be a field defaulting to 1, and every assignment
     * that left it off landed on round 1 — a form could carry three round 1s and a round 2.
     *
     * <p><b>Which ends "a round can hold more than one reviewer".</b> An interview and an entrance
     * test are now rounds 1 and 2 rather than two reviews of round 1, and they cannot run at the
     * same time.
     *
     * <p><b>One round at a time.</b> A form with a {@code PENDING} or {@code IN_PROGRESS} review
     * anywhere on it is {@code 409 REVIEW_STILL_OPEN} — finish it with #27c or call it off with
     * #27d first. A round is a stage, and two open at once is what made the rounds meaningless.
     *
     * <p><b>It moves the application to {@code UNDER_REVIEW}, and only from {@code SUBMITTED}.</b>
     * A later round moves nothing, and
     * {@code ADDITIONAL_INFORMATION_REQUIRED → UNDER_REVIEW} is #20's move rather than this one's.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND        no application with that id in this school
     * 409 APPLICATION_NOT_REVIEWABLE   a DRAFT nobody sent, or a form already decided
     * 404 STAFF_NOT_FOUND              a reviewer who is not this school's staff
     * 409 REVIEW_STILL_OPEN            a review on this form is PENDING or IN_PROGRESS
     * 400 VALIDATION_FAILED            a missing reviewer or role
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
     * Endpoint #27b — the reviewer has started looking.
     *
     * <p><b>{@code PENDING → IN_PROGRESS}, and nothing else.</b> #27 can make the same move inside
     * a general edit; this exists because <i>starting</i> is an event rather than a field being
     * set, and this module's rule is that events get a verb. It is what opening the review fires
     * on its own, which a {@code PATCH} carrying a status would be a strange shape for.
     *
     * <p><b>No body.</b> The id in the path is the whole request.
     *
     * <p><b>Not idempotent, deliberately.</b> Starting something already {@code IN_PROGRESS} is a
     * refusal, not a shrug: the caller believed they were picking up work nobody had, and a silent
     * 200 would hide that two people are on it.
     *
     * <p><b>A review is never {@code APPROVED} or {@code REJECTED}</b> — those are the
     * application's status (#20) and the reviewer's recommendation. The four this guards are the
     * four {@code AdmissionReviewStatus} has.
     *
     * <pre>
     * 404 REVIEW_NOT_FOUND             no review with that id in this school
     * 409 REVIEW_ALREADY_COMPLETED     it is done, and a record is not a draft to pick back up
     * 409 REVIEW_CANCELLED             the school called it off
     * 409 INVALID_REVIEW_TRANSITION    somebody already started it
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/reviews/{admissionReviewId}/start")
    public ResponseEntity<AdmissionReviewResponse> start(
            @PathVariable String admissionReviewId) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The review's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(admissionReviewService.startReview(admissionReviewId));
    }

    /**
     * Endpoint #27 — what the reviewer found.
     *
     * <p><b>Findings only, from 2026-09-30:</b> the {@code score} and the
     * {@code criterionScores} behind it. A number, and the numbers behind the number.
     *
     * <p><b>The {@code notes} went to #27e the same day.</b> What a reviewer writes is their
     * reasoning, and reasoning belongs beside the verdict it justifies rather than beside the
     * marks it is drawn from — a note saved here sat orphaned from the conclusion it explains.
     *
     * <p><b>Its request was renamed with it</b>, from {@code AdmissionReviewUpdateRequest} to
     * {@link com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewAddReviewRequest}.
     * "Update" described a general edit that could set any field including the ones that end the
     * review; what is left adds to the record of what somebody saw.
     *
     * <p><b>It used to carry the status and the recommendation too</b>, and neither belonged in a
     * general edit. Ending a review is something that HAPPENS, so it gets a verb — #27c finishes
     * it, #27d calls it off. The recommendation is the one thing a review exists to produce, and
     * it has #27e. This endpoint carried four refusals that belonged to three other decisions.
     *
     * <p><b>Only what you send moves</b>, so a reviewer can save a score today and add a note
     * tomorrow. A body that carries nothing is {@code 400 NOTHING_TO_UPDATE} rather than a silent
     * 200.
     *
     * <p><b>A finished review is a record.</b> Both {@code COMPLETED} and {@code CANCELLED} refuse
     * this; a score typed wrong is corrected by cancelling the review and assigning another, which
     * leaves both in the history rather than overwriting one.
     *
     * <p><b>Addressed by its own id</b>, not under the application it belongs to. A reviewer opens
     * their own queue far more often than they walk down from a form.
     *
     * <pre>
     * 404 REVIEW_NOT_FOUND             no review with that id in this school
     * 409 REVIEW_ALREADY_COMPLETED     it is done, and a record is not a draft
     * 409 REVIEW_CANCELLED             the school called it off
     * 400 NOTHING_TO_UPDATE            a body that changes nothing
     * 400 VALIDATION_FAILED            a negative score, or more than 50 criteria
     * 409 CONCURRENT_MODIFICATION      somebody recorded on it while you were reading
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PatchMapping("/reviews/{admissionReviewId}")
    public ResponseEntity<AdmissionReviewResponse> record(
            @PathVariable String admissionReviewId,
            @Valid @RequestBody AdmissionReviewAddReviewRequest request) {

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
     * Endpoint #27e — what the reviewer concludes.
     *
     * <p><b>The verdict, and the reasoning behind it.</b> APPROVE, REJECT, WAITLIST or
     * REQUEST_MORE_INFORMATION — required, because a body without one is not a partial
     * recommendation, it is a caller who has not said anything. The {@code notes} are optional and
     * moved here from #27 on 2026-09-30: reasoning belongs beside the verdict it justifies.
     *
     * <p><b>It does not finish the review</b> and does not stamp {@code completedAt}. Deciding
     * what you think and declaring yourself done are two decisions, often days apart: a reviewer
     * who has seen the child but wants to compare against the rest of the round has an answer and
     * is not finished. #27c is what ends it.
     *
     * <p><b>Recording a second one replaces the first.</b> Until the review ends this is a working
     * answer rather than a record, and a reviewer changing their mind before they finish is not
     * worth its own history.
     *
     * <p><b>Why it is not a field on #27.</b> A score is a measurement and a note is a remark; the
     * recommendation is what the reviewer makes of them, and it is the one thing a review exists
     * to produce. Setting it quietly inside a general PATCH put the module's most consequential
     * field in with its least.
     *
     * <p><b>#27c still takes one of its own</b>, for the reviewer who makes both decisions at
     * once. Finishing is an event that may insist on what it needs, and forcing two calls to end a
     * review would be ceremony rather than clarity.
     *
     * <pre>
     * 404 REVIEW_NOT_FOUND             no review with that id in this school
     * 409 REVIEW_ALREADY_COMPLETED     it is done, and a record is not a draft
     * 409 REVIEW_CANCELLED             the school called it off
     * 400 VALIDATION_FAILED            no recommendation, or one outside the enum
     * 409 CONCURRENT_MODIFICATION      somebody recorded on it while you were reading
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/reviews/{admissionReviewId}/recommendation")
    public ResponseEntity<AdmissionReviewResponse> recommend(
            @PathVariable String admissionReviewId,
            @Valid @RequestBody AdmissionReviewRecommendationRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The review's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionReviewService.recommend(admissionReviewId, request));
    }

    /**
     * Endpoint #27c — the reviewer is finished, and this is what they concluded.
     *
     * <p><b>To {@code COMPLETED}, and the body is the version alone — narrowed 2026-09-30.</b>
     * It carried a score, criterion scores, notes and a recommendation. The findings are #27's and
     * the verdict is #27e's; what is left of finishing is saying it is finished.
     *
     * <p><b>It refuses a review that has never been given a verdict</b> —
     * {@code 400 RECOMMENDATION_REQUIRED}. The rule did not go with the field: it is read off the
     * REVIEW rather than the body, so there is no way to smuggle one in. Record it with #27e
     * first.
     *
     * <p><b>And it refuses one recommending {@code REQUEST_MORE_INFORMATION}</b> —
     * {@code 409 RECOMMENDATION_NOT_FINAL}. Only APPROVE, REJECT and WAITLIST can be a review's
     * last word; the fourth is a reviewer asking for something, and the enum says so itself. Not a
     * dead end: when the answer arrives, #27e replaces it and the review finishes then.
     *
     * <p><b>It stamps {@code completedAt}</b>, which nothing else in this module does.
     *

     *
     * <p><b>No {@code NOTHING_TO_UPDATE}.</b> That is the difference between a verb and a
     * {@code PATCH} — an empty body here still asks for the move the path names.
     *
     * <p><b>It does not touch the application.</b> A completed review is one person's opinion; the
     * school's decision is #20, which reads none of these.
     *
     * <pre>
     * 404 REVIEW_NOT_FOUND             no review with that id in this school
     * 409 REVIEW_ALREADY_COMPLETED     it is done, and a record is not a draft
     * 409 REVIEW_CANCELLED             the school called it off
     * 409 INVALID_REVIEW_TRANSITION    not a move it can make from where it is
     * 400 RECOMMENDATION_REQUIRED      the review has never been given one — #27e records it
     * 409 RECOMMENDATION_NOT_FINAL     it recommends REQUEST_MORE_INFORMATION, which cannot end it
     * 400 VALIDATION_FAILED            a negative score, or more than 50 criteria
     * 409 CONCURRENT_MODIFICATION      somebody recorded on it while you were reading
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/reviews/{admissionReviewId}/complete")
    public ResponseEntity<AdmissionReviewResponse> complete(
            @PathVariable String admissionReviewId,
            @Valid @RequestBody AdmissionReviewCompleteRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The review's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionReviewService.completeReview(admissionReviewId, request));
    }

    /**
     * Endpoint #27d — the school called the review off.
     *
     * <p><b>To {@code CANCELLED}, with a reason.</b> The reviewer left, the round was assigned by
     * mistake, the family withdrew. A {@code PENDING} row nobody is ever going to work is worse
     * than a cancelled one: it sits in #28's queue for ever and makes the backlog a lie.
     *
     * <p><b>This is what a {@code DELETE} would have been.</b> The review stays, and says it was
     * called off and why.
     *
     * <p><b>The reason is required</b>, the same as {@code lostReason} on a lost inquiry. Work
     * abandoned with nothing said is a gap in the record.
     *
     * <p><b>It does not stamp {@code completedAt}</b>, and it moves nothing else. Whatever score or
     * recommendation was already recorded stays — that is the history being written into.
     *
     * <pre>
     * 404 REVIEW_NOT_FOUND             no review with that id in this school
     * 409 REVIEW_ALREADY_COMPLETED     it is done; the school undoes it in #20, not here
     * 409 REVIEW_CANCELLED             it was already called off
     * 409 INVALID_REVIEW_TRANSITION    not a move it can make from where it is
     * 400 CANCELLATION_NOTE_REQUIRED   cancelling without saying why
     * 409 CONCURRENT_MODIFICATION      somebody recorded on it while you were reading
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/reviews/{admissionReviewId}/cancel")
    public ResponseEntity<AdmissionReviewResponse> cancel(
            @PathVariable String admissionReviewId,
            @Valid @RequestBody AdmissionReviewCancelRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The review's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionReviewService.cancelReview(admissionReviewId, request));
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
