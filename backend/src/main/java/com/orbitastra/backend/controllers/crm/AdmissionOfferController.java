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
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferCreateRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionOfferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The seat a school formally offers. Endpoint #29 of the plan in this package's README; #30, #31
 * and #32 are not built.
 *
 * <p><b>Its own controller, because {@code admission_offers} is its own collection.</b> Five
 * collections get five controllers — the call this module's plan made after watching {@code people}
 * grow to fifteen endpoints across two documents in one file.
 *
 * <p><b>The base path is {@code /schools/current} and not a collection</b>, for the same reason the
 * review controller's is: an offer is <i>created</i> under the application it is for, because an
 * offer has no meaning apart from that form — but it is then answered and withdrawn by its own id
 * (#30, #31), because a family's reply arrives against an offer number rather than a form.
 *
 * <p><b>There is no {@code DELETE}.</b> An offer issued in error is withdrawn (#31), and one
 * replaced by a better one is {@code SUPERSEDED} — automatically, by #29. Admissions keeps what it
 * promised a family, including the promise it changed.
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
     * <p><b>A later offer supersedes the last, and every revision is kept.</b> "What did we
     * originally offer this family" is a question schools get asked, and an endpoint that
     * overwrote the previous row could not answer it. {@code revisionNo} is {@code max + 1}, worked
     * out from what is stored rather than sent — a caller-supplied revision is a caller who can
     * rewrite that history.
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
     * <pre>
     * 404 APPLICATION_NOT_FOUND        no application with that id in this school
     * 409 APPLICATION_NOT_APPROVED     not APPROVED, WAITLISTED or already OFFERED
     * 404 ADMISSION_CYCLE_NOT_FOUND    the round the form names is gone
     * 404 CLASS_NOT_FOUND              no such class in the cycle's academic year
     * 409 CLASS_NOT_IN_CAPACITY        the round has no seats set up for it
     * 404 STAFF_NOT_FOUND              an issuer who is not this school's staff
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
}
