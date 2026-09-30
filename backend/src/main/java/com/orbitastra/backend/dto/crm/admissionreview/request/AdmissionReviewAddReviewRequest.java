package com.orbitastra.backend.dto.crm.admissionreview.request;

import java.math.BigDecimal;
import java.util.Map;


import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * What a reviewer found. Endpoint #27.
 *
 * <h2>Findings only — narrowed 2026-09-30</h2>
 *
 * <p><b>It was {@code AdmissionReviewUpdateRequest} until this narrowing, and the name is part of
 * the change.</b> "Update" described a general edit that could set any field on the review,
 * including ending it; what is left adds to the record of what somebody saw. The endpoint is still
 * a {@code PATCH} because it merges rather than replaces — only what you send moves — but nothing
 * it carries decides anything.
 *
 * <p><b>Three fields, and all of them are measurements or remarks:</b> the {@code score}, the
 * {@code criterionScores} behind it, and the {@code notes}. That is what "what was found" means.
 *
 * <p><b>It used to carry the {@code status} and the {@code recommendation} too</b>, and neither
 * belonged here. Moving a review to {@code COMPLETED} or {@code CANCELLED} is something that
 * HAPPENS, and the module's rule is that those get a verb: #27b starts a review, #27c finishes it
 * and stamps {@code completedAt}, #27d calls it off and insists on a reason. The recommendation is
 * the one thing a review exists to produce, and it now has #27e. A general edit that could also
 * end the review meant one endpoint with four refusals belonging to three different decisions.
 *
 * <p><b>Only what you send moves</b>, the same as #2 on a cycle. Every field here is optional, and
 * an absent one is left exactly as it was — which is what lets a reviewer save a score today and
 * add to it tomorrow.
 *
 * <p><b>A body that changes nothing is a {@code 400}</b>, not a silent 200: a no-op that answers
 * 200 cannot be told apart from a change that worked.
 */
public record AdmissionReviewAddReviewRequest(


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
         * The overall mark. Example: 86.50
         *
         * <p><b>No upper bound, because the scale is the school's.</b> Out of 100, out of 50, out
         * of 5 — none of those is this module's business, and a cap would refuse a school for
         * marking differently. Negative is refused because it is not a scale, it is a typo, and the
         * digit limits are a typo guard for the same reason.
         */
        @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal score,


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
