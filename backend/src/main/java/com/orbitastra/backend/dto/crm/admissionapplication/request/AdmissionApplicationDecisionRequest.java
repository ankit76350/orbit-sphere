package com.orbitastra.backend.dto.crm.admissionapplication.request;

import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the school decided about an application. Endpoint #20.
 *
 * <p><b>It names the status the application is moving to</b>, exactly as #3 does for a cycle. There
 * is no separate decision vocabulary: {@code APPROVED} is the value, not {@code APPROVE}, so a
 * caller reads one set of words on the way in and the same set on the way out.
 *
 * <p><b>An earlier draft of this took its own five-value enum</b> — {@code APPROVE},
 * {@code REJECT}, {@code WAITLIST}, {@code REQUEST_MORE_INFORMATION}, {@code RESUME_REVIEW} — on
 * the argument that a body naming a status could write {@code ENROLLED} onto a form nobody had
 * offered a seat to. <b>That argument was wrong</b>: the transition table is what refuses
 * {@code ENROLLED}, not the shape of the vocabulary, and the module had already settled the
 * question on #3. The parallel enum bought nothing and made the caller learn two names for every
 * move.
 *
 * <p><b>It does not require a completed review, and that is deliberate.</b> Small schools decide in
 * a conversation; an endpoint that insisted on a review row would make them invent one. So this can
 * be sent against a form that has never been near #26 — which is why {@code SUBMITTED} is one of
 * the statuses it accepts.
 */
public record AdmissionApplicationDecisionRequest(

        /**
         * Where the application is moving to. Example: APPROVED
         *
         * <p>Must be a move the status graph has <b>from where the form actually is</b> — the
         * refusal lists what is reachable, and when nothing is, says why.
         *
         * <p><b>Not every value of the enum is something this endpoint can set.</b>
         * {@code OFFERED}, {@code OFFER_ACCEPTED} and {@code ENROLLED} are consequences of #29, #30
         * and #33; {@code WITHDRAWN} is #21's; {@code DRAFT} and {@code SUBMITTED} are the family's
         * side. Asking for any of them here is a refusal, not a shortcut.
         */
        @NotNull AdmissionApplicationStatus status,

        /**
         * Why, in the school's words. Example: "Interview scores below the cut-off for Grade 7"
         *
         * <p><b>Required for {@code REJECTED} and for {@code ADDITIONAL_INFORMATION_REQUIRED}</b>,
         * optional otherwise. The plan asked for it on the rejection alone; the second was added
         * when this was built, because asking a family for more without saying what tells them
         * nothing — the same reading that makes {@code lostReason} required on a lost inquiry.
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
