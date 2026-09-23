package com.orbitastra.backend.dto.crm.admissionreview.request;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
         * Which round of review this is. Example: 1
         *
         * <p><b>Absent means round 1</b>, which is the common case — most applications are looked
         * at once. A round can hold <b>more than one reviewer</b>: an interview and an entrance
         * test on the same day are two reviews of round 1, which is why the uniqueness rule is on
         * the round <i>and</i> the reviewer rather than on the round alone.
         *
         * <p><b>Rounds run 1, 2, 3 with no gaps.</b> Round 3 needs a round 2 to already exist on
         * this application, by <i>anybody</i> — a second assessor joining round 1 does not open
         * round 2, because the rounds are the school's stages rather than one person's. Asking for
         * a round with nothing before it is {@code REVIEW_ROUND_OUT_OF_ORDER}.
         *
         * <p>This was unchecked until 2026-09-23, on the grounds that a school numbering its
         * rounds 1 and 3 was odd rather than wrong. It is wrong: a gap is somebody typing the
         * wrong number, and the round it leaves behind can never be filled in afterwards.
         *
         * <p>The cap of 20 is a typo guard — a school does not review an application twenty-one
         * times, and {@code 2026} in this field is somebody's mistake rather than a round.
         */
        @Min(1) @Max(20) Integer reviewRound,

        /**
         * When the review is wanted by. Example: "2026-03-15T17:00:00Z"
         *
         * <p>Optional, and <b>a date in the past is accepted</b>. A school catching up on paperwork
         * records a review that was due last week, and refusing that would make the backlog
         * unrecordable. It is what #28's queue sorts on.
         */
        Instant dueAt,

        /** Anything to tell the reviewer. Optional. */
        @Size(max = 2000) String notes) {
}
