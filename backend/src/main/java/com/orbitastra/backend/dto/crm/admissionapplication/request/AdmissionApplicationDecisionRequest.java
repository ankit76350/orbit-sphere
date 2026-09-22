package com.orbitastra.backend.dto.crm.admissionapplication.request;

import com.orbitastra.backend.models.crm.enums.AdmissionDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the school decided about an application. Endpoint #20.
 *
 * <p><b>It does not require a completed review, and that is deliberate.</b> Small schools decide in
 * a conversation; an endpoint that insisted on a review row would make them invent one. So this can
 * be sent against a form that has never been near #26 — which is why {@code SUBMITTED} is one of
 * the statuses it accepts.
 *
 * <p><b>There is no {@code status} field here.</b> The caller says what they are <i>doing</i> and
 * the endpoint works out where that leaves the application. A body that named the status directly
 * would let a caller write {@code ENROLLED} onto a form nobody had offered a seat to.
 */
public record AdmissionApplicationDecisionRequest(

        /**
         * What the school is doing. Example: APPROVE
         *
         * <p>Must be legal from where the application is — {@code WAITLIST} on something already
         * waitlisted moves nothing, and the refusal lists what is reachable.
         */
        @NotNull AdmissionDecision decision,

        /**
         * Why, in the school's words. Example: "Interview scores below the cut-off for Grade 7"
         *
         * <p><b>Required for {@code REJECT} and for {@code REQUEST_MORE_INFORMATION}</b>, optional
         * otherwise. The plan asked for it on {@code REJECT} alone; the second was added when this
         * was built, because asking a family for more without saying what tells them nothing — the
         * same reading that makes {@code lostReason} required on a lost inquiry.
         *
         * <p>It is <b>kept</b> on the application, not logged and dropped. Admissions is the record
         * of what a school decided about a child, and a refusal with no reason is the part of that
         * record worth the most.
         */
        @Size(max = 2000) String note,

        /**
         * The version last read. Optional.
         *
         * <p>Sent → a form somebody else decided in the meantime answers
         * {@code 409 CONCURRENT_MODIFICATION} rather than being decided twice. Absent → last write
         * wins.
         */
        Long version) {
}
