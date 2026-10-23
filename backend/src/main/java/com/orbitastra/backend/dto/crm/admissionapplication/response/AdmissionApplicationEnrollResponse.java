package com.orbitastra.backend.dto.crm.admissionapplication.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.dto.student.student.response.StudentResponse;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

/**
 * What #33 answers with — <b>the handover, written down</b>.
 *
 * <p><b>It names everything that moved, because four documents moved.</b> A child was created, the
 * application was linked to them and closed, and the lead that started it was closed too. A
 * response that gave back only the application would leave the caller to go and find out what else
 * had happened, which for the one endpoint that writes across two modules is exactly the wrong
 * answer.
 *
 * <p><b>The student comes back whole, not as an id.</b> This is the only moment the child is new,
 * and the admission number on it is the thing somebody writes on a certificate. Making the caller
 * fetch {@code student} #5 to read a number this request just generated would be asking them to
 * do a round trip for information we already have in hand.
 */
public record AdmissionApplicationEnrollResponse(

        String admissionApplicationId,
        String applicationNo,
        AdmissionApplicationStatus status,
        String admissionCycleDocsId,
        String appliedClassDocsId,

        /** Resolved where it can be. Absent when the class has been removed since. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String appliedClassName,

        /** The child. New as of this request. */
        StudentResponse student,

        /**
         * The offer that made this legal — the {@code ACCEPTED} one.
         *
         * <p><b>Reported, not changed.</b> See {@code enrollApplicant} for why there is nothing to
         * move it to: {@code ACCEPTED} is where an offer ends when everything goes right.
         */
        String acceptedOfferDocsId,
        String acceptedOfferNo,

        /**
         * The lead this family started as, now {@code CLOSED}.
         *
         * <p>Absent for a walk-in, which is most of them — a family that arrived with a completed
         * form never enquired, and that is the normal case this whole module is built to allow.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String inquiryDocsId,

        /**
         * Whether the lead was actually closed.
         *
         * <p>Absent when there was no lead. <b>False means the form named one that is not in this
         * school any more</b> — the child is still enrolled, and this says plainly that the lead
         * was not closed rather than leaving somebody to wonder.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean inquiryClosed,

        Instant enrolledAt,
        Long version,
        String nextStep) {

    public static AdmissionApplicationEnrollResponse of(AdmissionApplication application,
            String appliedClassName, StudentResponse student, String acceptedOfferDocsId,
            String acceptedOfferNo, Boolean inquiryClosed, String nextStep) {

        return new AdmissionApplicationEnrollResponse(
                application.getId(),
                application.getApplicationNo(),
                application.getStatus(),
                application.getAdmissionCycleDocsId(),
                application.getAppliedClassDocsId(),
                appliedClassName,
                student,
                acceptedOfferDocsId,
                acceptedOfferNo,
                application.getInquiryDocsId(),
                inquiryClosed,
                application.getUpdatedAt(),
                application.getVersion(),
                nextStep);
    }
}
