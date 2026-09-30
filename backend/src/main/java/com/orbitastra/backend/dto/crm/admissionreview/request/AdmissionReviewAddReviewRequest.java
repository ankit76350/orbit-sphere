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
 * <p><b>Two fields, and both are measurements:</b> the {@code score} and the
 * {@code criterionScores} behind it. That is what "what was found" means — a number, and the
 * numbers behind the number.
 *
 * <p><b>The {@code notes} left on 2026-09-30 too</b>, for #27e. What a reviewer writes is their
 * reasoning, and reasoning belongs beside the verdict it justifies rather than beside the marks it
 * is drawn from — a note saved here would have sat orphaned from the conclusion it explains.
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
         * What each part came to, as the school writes it.
         * Example: {@code { "INTERVIEW": "42.50", "ENTRANCE_TEST": "B+", "READING": "Pass" }}
         *
         * <p><b>The values are STRINGS from 2026-09-30, not numbers.</b> A criterion result is not
         * always a figure: a school grades an interview A/B/C, marks a reading check Pass or Fail,
         * and writes "42/50" for a paper. {@code BigDecimal} refused every one of those — the same
         * mistake {@code score} avoided by having no upper bound, because the scale is the
         * school's.
         *
         * <p><b>Which means nothing checks the value any more.</b> "abc" is storable where it used
         * to be {@code 400 MALFORMED_REQUEST} from the JSON reader, and no total can be computed
         * across the map without parsing it first. Neither was being done, and the length cap is
         * all that is left to bound.
         *
         * <p><b>Sent replaces the whole map</b>, it does not merge into it — a map is one value,
         * and merging would leave no way to remove a criterion that was recorded by mistake.
         * {@code {}} clears it; absent leaves it alone.
         *
         * <p><b>Nothing checks the keys either.</b> There is no criterion-definition model, so a
         * school names its own parts. What can be bounded is how many there are, and how long each
         * value is.
         */
        @Size(max = 50) Map<String, @Size(max = 40) String> criterionScores,


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
         * The version last read. Optional.
         *
         * <p>Sent → a review somebody else recorded in the meantime answers
         * {@code 409 CONCURRENT_MODIFICATION}. Absent → last write wins.
         */
        Long version) {
}
