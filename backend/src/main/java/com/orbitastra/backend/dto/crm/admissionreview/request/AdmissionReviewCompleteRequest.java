package com.orbitastra.backend.dto.crm.admissionreview.request;

import java.math.BigDecimal;
import java.util.Map;

import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * What #27c carries — the reviewer is finished, and this is what they concluded.
 *
 * <p><b>The same fields #27 has, minus the status.</b> The status is the endpoint: a call to
 * {@code /complete} is the move, so a body naming one would be a second way to say the same thing
 * and a way to disagree with the path.
 *
 * <p><b>{@code recommendation} is NOT {@code @NotNull} here</b>, and that is deliberate rather than
 * an oversight. A reviewer who already saved one with #27 should not have to send it again, so the
 * rule is "the review has one by the time it is done" rather than "this body carries one" — which
 * is exactly the rule #27 enforces, with exactly the same {@code 400 RECOMMENDATION_REQUIRED}. One
 * vocabulary, checked in the service where the review is in hand.
 *
 * <p><b>{@code criterionScores} REPLACES the map, it does not merge into it</b> — as on #27, since
 * merging would leave no way to remove a criterion recorded by mistake. Absent leaves it alone.
 *
 * <p><b>{@code version} is optional.</b> Send what you read for
 * {@code 409 CONCURRENT_MODIFICATION} when somebody recorded on the review while you were looking;
 * leave it out and the last write wins.
 */
public record AdmissionReviewCompleteRequest(

        AdmissionRecommendation recommendation,

        @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal score,

        @Size(max = 50) Map<String, @Digits(integer = 6, fraction = 2) BigDecimal> criterionScores,

        @Size(max = 2000) String notes,

        Long version) {
}
