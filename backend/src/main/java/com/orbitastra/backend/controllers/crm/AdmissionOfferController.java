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
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferCreateRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferRespondRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferSearchRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferUpdateRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferWithdrawRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferResponse;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionOfferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The seat a school formally offers, and what becomes of it. Endpoints #29, #30, #31 and #32 of
 * the plan in this package's README — all four built, which completes phase 4 — plus #29b, which
 * the plan did not have.
 *
 * <p><b>Its own controller, because {@code admission_offers} is its own collection.</b> Five
 * collections get five controllers — the call this module's plan made after watching {@code people}
 * grow to fifteen endpoints across two documents in one file.
 *
 * <p><b>The base path is {@code /schools/current} and not a collection</b>, for the same reason the
 * review controller's is: an offer is <i>created</i> under the application it is for, because an
 * offer has no meaning apart from that form — but it is then answered and withdrawn by its own id
 * (#30, #31), because a family's reply arrives against an offer number rather than a form, and
 * listed on its own (#32), because the chase list crosses every application at once.
 *
 * <p><b>There is no {@code DELETE}.</b> An offer issued in error is withdrawn (#31). Admissions
 * keeps what it promised a family, including the promise it changed — and with <b>one letter per
 * admission</b>, that promise is a single document whose status says where it got to.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current")
public class AdmissionOfferController {

    private final AdmissionOfferService admissionOfferService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Writes run gates 1 and 2. <b>Gate 4 never runs in this module</b> — see the README. What
     * takes its place here is the <i>application's</i> status: a form the school has not said yes
     * to has no seat to offer.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #29 — the school offers a seat.
     *
     * <p><b>ONE OFFER PER APPLICATION.</b> A school issues one offer letter for one admission; if
     * it expires the school extends it, and if anything else changes the school edits it. A second
     * one is {@code 409 OFFER_ALREADY_ISSUED} whatever became of the first — a {@code WITHDRAWN} or
     * {@code DECLINED} offer is still that application's offer, and its status is the record of
     * what happened to it.
     *
     * <p><b>The declared index says the same thing.</b> {@code revisionNo} is pinned to 1, which
     * makes {@code school_application_offer_revision_uniq} mean "one offer per application". That
     * index is <b>declared and not built</b> in a development database — this project syncs
     * indexes on demand — so until it is, the service's check is the only thing enforcing it.
     *
     * <p><b>There is no endpoint to edit one yet.</b> Extending an expired offer is what #31's
     * neighbourhood will do; until then an offer that has gone stale cannot be moved.
     *
     * <p><b>It takes no status.</b> Issuing is the endpoint, so the offer is created
     * {@code ISSUED} and {@code offeredAt} is stamped.
     *
     * <p><b>The offered class is not always the applied class.</b> A school assesses a child and
     * offers a different grade. It must be a class of the cycle's year that the round has seats
     * set up for — #17's rule — but the number of offers is <b>not</b> capped against those seats:
     * schools deliberately over-offer, and #7 is what counts offers against places.
     *
     * <p><b>{@code expiresAt} defaults to the cycle's {@code enrollmentDeadlineAt}</b>, the date
     * the school already published for that round.
     *
     * <p><b>The application moves to {@code OFFERED} as a consequence</b>, not because anything
     * asked it to — the same shape as #26 moving a form to {@code UNDER_REVIEW}.
     *
     * <p><b>{@code depositInvoiceDocsId} is checked, and today it refuses everything.</b> An id
     * nothing verifies is an id that can be anything, so a named invoice has to exist in this
     * school — but nothing writes {@code fee_invoices} yet, so there is no id this will accept.
     * <b>The field is unusable until the finance module exists</b>, and refusing is the honest way
     * to say so. Leave it out.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND        no application with that id in this school
     * 409 APPLICATION_NOT_ELIGIBLE_FOR_OFFER   not APPROVED or WAITLISTED
     * 409 OFFER_ALREADY_ISSUED         this application already has its one offer
     * 404 ADMISSION_CYCLE_NOT_FOUND    the round the form names is gone
     * 404 CLASS_NOT_FOUND              no such class in the cycle's academic year
     * 409 CLASS_NOT_IN_CAPACITY        the round has no seats set up for it
     * 404 STAFF_NOT_FOUND              an issuer who is not this school's staff
     * 404 FEE_INVOICE_NOT_FOUND        a deposit invoice that is not there — see the note below
     * 400 OFFER_EXPIRY_IN_THE_PAST     a deadline that has already passed
     * 400 VALIDATION_FAILED            no offered class, or a blank one
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/applications/{admissionApplicationId}/offers")
    public ResponseEntity<AdmissionOfferResponse> issue(
            @PathVariable String admissionApplicationId,
            @Valid @RequestBody AdmissionOfferCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The application's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        AdmissionOfferResponse response =
                admissionOfferService.issueOffer(admissionApplicationId, request);

        //! Addressed by its OWN id from here on — #30 and #31 are /offers/{id} — which is why
        //! this Location is not the path the request was made to.
        return ResponseEntity
                .created(URI.create("/schools/current/offers/" + response.admissionOfferId()))
                .body(response);
    }

    /**
     * Endpoint #29b — correcting the one offer letter this admission has.
     *
     * <p><b>It exists because the one-offer rule opened a hole.</b> When the single letter lapsed,
     * nothing could extend it and #29 could not issue another, so a family that missed the deadline
     * could not be given a seat by any route. A dead end in something already shipped.
     *
     * <p><b>Extending a lapsed offer works</b>, and that is the point: nothing writes
     * {@code EXPIRED}, so a lapsed offer is still stored as {@code ISSUED} and is still reachable.
     *
     * <p><b>A {@code PATCH}, not a verb.</b> This module gives verbs to <i>events</i>; correcting a
     * letter is fields being set, which is what #27 is for reviews.
     *
     * <p><b>Only an {@code ISSUED} offer.</b> Once a family has answered, changing the deadline or
     * the grade underneath them rewrites what they agreed to.
     *
     * <p><b>It cannot set a status or a response</b> — those are #30's and #31's — and it does not
     * move {@code offeredAt}: a correction is not a reissue.
     *
     * <pre>
     * 404 OFFER_NOT_FOUND              no offer with that id in this school
     * 400 NOTHING_TO_UPDATE            a body that changes nothing
     * 409 OFFER_NOT_OPEN               already answered, or withdrawn
     * 400 OFFER_EXPIRY_IN_THE_PAST     extending it into the past is not an extension
     * 404 CLASS_NOT_FOUND              no such class in the cycle's year
     * 409 CLASS_NOT_IN_CAPACITY        the round has no seats set up for it
     * 404 FEE_INVOICE_NOT_FOUND        a deposit invoice that is not there
     * 409 CONCURRENT_MODIFICATION      somebody moved it while you were reading
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PatchMapping("/offers/{admissionOfferId}")
    public ResponseEntity<AdmissionOfferResponse> correct(
            @PathVariable String admissionOfferId,
            @Valid @RequestBody AdmissionOfferUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The offer's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(admissionOfferService.updateOffer(admissionOfferId, request));
    }

    /**
     * Endpoint #30 — the family answers.
     *
     * <p><b>This is why the offer half exists.</b> Approving is the school saying yes; this is the
     * family saying yes, and without it a school cannot tell an approved child who is coming from
     * one who went elsewhere.
     *
     * <p><b>It takes the ANSWER, not a status.</b> {@code ACCEPTED} or {@code DECLINED} — two
     * values, which the endpoint maps onto the offer's own statuses. Different from #20, where the
     * school is choosing among its statuses; here the family is choosing between yes and no.
     *
     * <p><b>Only an {@code ISSUED} offer, and only before it lapses.</b> An offer past its
     * {@code expiresAt} is refused even though its stored status still reads {@code ISSUED} —
     * {@code EXPIRED} is what a date in the past MEANS and nothing writes it.
     *
     * <p><b>{@code ACCEPTED} moves the application to {@code OFFER_ACCEPTED}; {@code DECLINED}
     * moves nothing.</b> A declined offer is <i>not</i> a rejected applicant — the school decided
     * to admit this child and the family chose otherwise, and those are different facts.
     *
     * <pre>
     * 404 OFFER_NOT_FOUND              no offer with that id in this school
     * 409 OFFER_NOT_OPEN               already answered, withdrawn, or never issued
     * 409 OFFER_EXPIRED                past its date — and only the clock says so
     * 409 CONCURRENT_MODIFICATION      somebody moved it while you were reading
     * 400 VALIDATION_FAILED            no answer, or one that is not on the enum
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/offers/{admissionOfferId}/respond")
    public ResponseEntity<AdmissionOfferResponse> respond(
            @PathVariable String admissionOfferId,
            @Valid @RequestBody AdmissionOfferRespondRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The offer's own status and date are what decide, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(admissionOfferService.respond(admissionOfferId, request));
    }

    /**
     * Endpoint #31 — the school takes the offer back.
     *
     * <p><b>A reason is required.</b> A seat promised to a family and then taken away is exactly
     * what somebody asks about later, and it is kept on the offer rather than logged and dropped.
     *
     * <p><b>Only an offer still out can be taken back.</b> An {@code ACCEPTED} one is refused, and
     * that is the interesting line: the family holds the seat, and taking it away is a decision
     * about the <i>application</i> (#20) rather than a tidy-up of the letter.
     *
     * <p><b>It does not touch the application.</b> Withdrawing an offer does not un-approve a
     * child.
     *
     * <p><b>It does not stamp {@code respondedAt}</b> — the family did not answer, the school
     * changed its mind, and stamping it would make a withdrawal read as a decline in every list.
     *
     * <pre>
     * 404 OFFER_NOT_FOUND              no offer with that id in this school
     * 409 OFFER_NOT_OPEN               already answered, or already withdrawn
     * 409 CONCURRENT_MODIFICATION      somebody moved it while you were reading
     * 400 VALIDATION_FAILED            no reason, or a blank one
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PostMapping("/offers/{admissionOfferId}/withdraw")
    public ResponseEntity<AdmissionOfferResponse> withdrawOffer(
            @PathVariable String admissionOfferId,
            @Valid @RequestBody AdmissionOfferWithdrawRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The offer's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(admissionOfferService.withdraw(admissionOfferId, request));
    }

    /**
     * Endpoint #32 — <b>what is expiring</b>. The chase list.
     *
     * <p><b>Soonest to lapse first</b>, which is the whole of what this is. Filter by
     * {@code status} and {@code expiringBefore} and you have this week's phone calls — those two
     * are exactly {@code school_offer_status_expiry_idx}.
     *
     * <p><b>{@code expired=true} is the sharper question</b>: past its date <b>and</b> still
     * {@code ISSUED}, because an offer a family accepted last month also has a past date.
     *
     * <p><b>An offer with no expiry is never expired</b>, and sorts to the front by default because
     * Mongo puts a missing field first. That is the wrong end of a chase list; the filter is what
     * answers "what has lapsed", not the order.
     *
     * <p><b>A row is thinner than the offer</b>: no withdrawal reason, no document ids. The reason
     * is something a school wrote about one family, and a page of twenty would carry all of it to
     * draw a list that shows none of it.
     *
     * <p><b>But the applicant IS named</b>, in one query for the whole page. A chase list of raw
     * ids is not a list anybody can work from — the point of it is to ring people.
     *
     * <p><b>No gates.</b> A read — a suspended school still needs to know what it promised.
     *
     * <pre>
     * 400 INVALID_PAGE           a negative page
     * 400 INVALID_PAGE_SIZE      a size below 1 or above 100
     * 400 INVALID_SORT_FIELD     a field that is not on the allowlist
     * 400 TENANT_NOT_RESOLVED    no idtoken cookie
     * </pre>
     */
    @GetMapping("/offers")
    public ResponseEntity<PageResponse<AdmissionOfferSummaryResponse>> list(
            AdmissionOfferSearchRequest request) {

        //! NO GATES. Reads run none: a school that cannot be edited still promised these seats,
        //! and hiding them would make the chase list disappear exactly when it matters.
        return ResponseEntity.ok(admissionOfferService.listOffers(request));
    }
}
