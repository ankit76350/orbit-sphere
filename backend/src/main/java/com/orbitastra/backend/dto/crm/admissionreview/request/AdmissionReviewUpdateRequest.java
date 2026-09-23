package com.orbitastra.backend.dto.crm.admissionreview.request;

import java.math.BigDecimal;
import java.util.Map;

import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * What a reviewer found. Endpoint #27.
 *
 * <p><b>Only what you send moves</b>, the same as #2 on a cycle. Every field here is optional, and
 * an absent one is left exactly as it was — which is what lets a reviewer save a score today and
 * add the recommendation tomorrow.
 *
 * <p><b>A body that changes nothing is a {@code 400}</b>, not a silent 200: a no-op that answers
 * 200 cannot be told apart from a change that worked.
 *
 * <p><b>The status is named directly</b>, as #3 and #20 do. There is no separate verb for
 * "complete it" — moving it to {@code COMPLETED} is the completion, and that is when
 * {@code completedAt} is stamped.
 */
public record AdmissionReviewUpdateRequest(

        /**
         * Where the review is moving to. Example: COMPLETED
         *
         * <p>Absent leaves it where it is, so a reviewer can record a part-finished score without
         * declaring themselves done.
         *
         * <p><b>{@code COMPLETED} requires a recommendation</b> — a finished review that does not
         * say what it recommends is the one thing a review exists to produce.
         * {@code CANCELLED} requires a note, by the same reading that makes {@code lostReason}
         * required on a lost inquiry.
         */
        AdmissionReviewStatus status,

        /**
         * The overall mark. Example: 86.50
         *
         * <p><b>No upper bound, because the scale is the school's.</b> Out of 100, out of 50, out
         * of 5 — none of those is this module's business, and a cap would refuse a school for
         * marking differently. Negative is refused because it is not a scale, it is a typo, and the
         * digit limits are a typo guard for the same reason.
         */
        @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal score,

        /**
         * What this reviewer suggests. Example: APPROVE
         *
         * <p><b>A recommendation, not a decision.</b> It is what one person thinks; #20 is what the
         * school does, and the school may decide something no reviewer recommended. That is why the
         * two are separate enums even though four of the values read alike.
         */
        AdmissionRecommendation recommendation,

        /**
         * What each part scored. Example: {@code { "INTERVIEW": 42.50, "ENTRANCE_TEST": 44.00 }}
         *
         * <p><b>Sent replaces the whole map</b>, it does not merge into it — a map is one value,
         * and merging would leave no way to remove a criterion that was recorded by mistake.
         * {@code {}} clears it; absent leaves it alone.
         *
         * <p><b>Nothing checks the keys.</b> There is no criterion-definition model, so a school
         * names its own parts. The only thing that can be bounded is how many there are.
         */
        @Size(max = 50) Map<String, @Digits(integer = 6, fraction = 2) BigDecimal> criterionScores,

        /**
         * What the reviewer wants to say. Example: "The applicant performed well."
         *
         * <p>{@code ""} clears it. Required when cancelling.
         */
        @Size(max = 2000) String notes,

        /**
         * The version last read. Optional.
         *
         * <p>Sent → a review somebody else recorded in the meantime answers
         * {@code 409 CONCURRENT_MODIFICATION}. Absent → last write wins.
         */
        Long version) {
}
