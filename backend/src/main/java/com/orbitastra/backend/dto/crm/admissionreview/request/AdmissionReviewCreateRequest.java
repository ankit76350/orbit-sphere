package com.orbitastra.backend.dto.crm.admissionreview.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Assign somebody to review an application. Endpoint #26.
 *
 * <p><b>This assigns the work; it does not do it.</b> The score, the criteria and the
 * recommendation are all #27, which records the result. A create that accepted them would make the
 * two endpoints interchangeable, and there would be nothing to say whether a review was outstanding
 * — which is the whole thing a reviewer's queue (#28) is built on.
 *
 * <p><b>The application is in the URL, not in here.</b> A review has no meaning apart from the form
 * it is of, so the address names it.
 */
public record AdmissionReviewCreateRequest(

        /**
         * Which member of staff is reviewing. Example: "67aa15d9dc3f7d0088888888"
         *
         * <p>Must be staff of <b>this school</b> — an id from another school is
         * {@code STAFF_NOT_FOUND}, not a reviewer.
         */
        @NotBlank @Size(max = 60) String reviewerDocsId,

        /**
         * What they are acting as. Example: "ADMISSION_OFFICER"
         *
         * <p><b>A free string, deliberately.</b> There is no reviewer-role enum, and the module's
         * README records that as an open item: schools run interviews, entrance tests and principal
         * rounds under names of their own, and an enum written now would be wrong within a month.
         * It is stored as sent.
         */
        @NotBlank @Size(max = 60) String reviewerRole,

        /**
         * When the review is wanted by. Example: "2026-03-15T17:00:00Z"
         *
         * <p>Optional, and <b>a date in the past is accepted</b>. A school catching up on paperwork
         * records a review that was due last week, and refusing that would make the backlog
         * unrecordable. It is what #28's queue sorts on.
         */
        Instant dueAt) {
}
