package com.orbitastra.backend.dto.plans.subscription.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * One subscription as it appears in a school's history. Endpoint #28.
 *
 * <p>A summary. The list is read to find a period — "when were they on Premium", "what did that
 * cancelled one cost" — not to work on one, so the plan's features are absent entirely and #27
 * returns them in full for the row the school is on now.
 *
 * <p><b>{@code current} is the field the list exists to disambiguate.</b> Every row here looks
 * like a subscription; exactly one of them is the one in force, and without the flag a caller
 * reading a page of five would have to compare dates to work out which.
 *
 * <p>{@code periodEnded} is beside it for the same reason, and it is <b>not</b> derivable from
 * {@code status}: nothing marks a lapsed subscription {@code EXPIRED} yet, so a row can read
 * {@code ACTIVE} with a period that finished months ago. Computing it here is what stops every
 * caller getting that wrong in its own way — the same reasoning as
 * {@link SubscriptionDetailResponse}.
 *
 * <h2>The plan is named, not just linked</h2>
 *
 * <p>{@code planCode}, {@code planVersion} and {@code planName} come from the plan version the
 * row points at. A history of {@code planDefinitionDocsId} values is unreadable — nobody can
 * tell from a Mongo id whether a school was on Premium v1 or Standard v3, which is the only
 * question a history is opened to answer.
 *
 * <p>The plans behind a page are fetched in <b>one</b> query and handed in, so a page of twenty
 * rows costs one plan read rather than twenty. See the note on the service.
 *
 * <p><b>{@code planDefinitionDocsId} is deliberately absent.</b> It is an internal document id
 * with no meaning to a caller, and everything it was needed for — which plan, which version — is
 * spelled out above it. {@code subscriptionId} stays because it is how #29 asks for a row's
 * history, and {@code subscriptionNo} because that is the number printed on the record.
 *
 * <p><b>{@code billingCustomerReference} is absent too</b>, and that is a privacy line rather
 * than a tidiness one: it is a payment-gateway customer id, it belongs in the single-subscription
 * read where somebody is actually working on billing, and a list is the wrong place to spray it
 * across twenty rows.
 */
public record SubscriptionSummaryResponse(
        String subscriptionId,
        String subscriptionNo,
        String planCode,
        Integer planVersion,
        String planName,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean periodEnded,
        Boolean autoRenew,
        Boolean current,
        BigDecimal contractedPrice,
        String currencyCode,
        String reasonForChanges,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param plan the version this row points at, or {@code null} when that plan document has
     *             gone. A missing plan leaves the three plan fields null rather than failing the
     *             whole page: a history is exactly where a since-deleted plan turns up, and one
     *             unreadable row is worth more than a 500 for the other nineteen.
     */
    public static SubscriptionSummaryResponse fromSubscription(SchoolSubscription subscription,
            PlanDefinition plan, Instant now) {

        Instant periodEnd = subscription.getCurrentPeriodEnd();

        return new SubscriptionSummaryResponse(
                subscription.getId(),
                subscription.getSubscriptionNo(),
                plan == null ? null : plan.getPlanCode(),
                subscription.getPlanVersion(),
                plan == null ? null : plan.getName(),
                subscription.getStatus(),
                subscription.getBillingCycle(),
                subscription.getCurrentPeriodStart(),
                periodEnd,
                periodEnd != null && !periodEnd.isAfter(now),
                subscription.getAutoRenew(),
                subscription.getCurrent(),
                subscription.getContractedPrice(),
                subscription.getCurrencyCode(),
                subscription.getReasonForChanges(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }
}
