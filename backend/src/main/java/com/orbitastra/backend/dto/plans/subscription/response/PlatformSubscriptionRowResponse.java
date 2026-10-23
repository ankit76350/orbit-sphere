package com.orbitastra.backend.dto.plans.subscription.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * One subscription as it appears in the platform-wide list. Endpoint #30.
 *
 * <h2>The school is named, and that is the difference from #28</h2>
 *
 * <p>{@link SubscriptionSummaryResponse} deliberately withholds {@code schoolId}, because #28 is
 * read one school at a time and the school is already in the URL. Here it is the opposite: a row
 * that did not say whose it was would be unreadable, so {@code schoolId},
 * {@code schoolName} and {@code subdomain} are all present. The name is what an operator scans;
 * the subdomain is how they reach the school; the id is what they put in #28's URL to drill in.
 *
 * <p><b>{@code schoolStatus} is here too, and it is not padding.</b> A school that is
 * {@code SUSPENDED} or {@code CLOSED} with an {@code ACTIVE} subscription is exactly the row an
 * operator's screen exists to surface, and the two statuses move independently — nothing keeps
 * them in step. It costs nothing: the school document is already read to get the name.
 *
 * <p>The schools behind a page are fetched in <b>one</b> query and handed in, so a page of twenty
 * rows costs one school read rather than twenty. Same for the plans. See the note on the service.
 *
 * <h2>What it carries beyond the school</h2>
 *
 * <p>{@code planCode}, {@code planVersion} and {@code planName} come from the plan version the row
 * points at, for the same reason as #28: a page of {@code planDefinitionDocsId} values cannot
 * answer "who is on Premium".
 *
 * <p>{@code periodEnded} is computed once for the whole page from a single {@code now}, and it is
 * <b>not</b> derivable from {@code status}: nothing marks a lapsed subscription {@code EXPIRED}
 * yet, so a row can read {@code ACTIVE} with a period that finished months ago. On a
 * platform-wide list that is the most important thing on the row — it is the difference between
 * "paying" and "nobody has noticed".
 *
 * <p>{@code current} says whether this is the row the school is on now. Without it a page of
 * periods looks like a page of schools; see the note on the request record.
 *
 * <h2>What is deliberately absent</h2>
 *
 * <p><b>{@code planDefinitionDocsId}</b> — an internal id with no meaning to a caller, and
 * everything it was needed for is spelled out above it.
 *
 * <p><b>{@code billingCustomerReference}</b> — a payment-gateway customer id. That is a privacy
 * line rather than a tidiness one, and spraying it across a list of every school on the platform
 * is the worst place for it. #27 returns it where somebody is actually working on one school's
 * billing.
 *
 * <p><b>The plan's features and the capacity overrides</b> — this list is read to find a
 * subscription, not to work on one. #27 returns those in full.
 */
public record PlatformSubscriptionRowResponse(
        String subscriptionId,
        String subscriptionNo,
        String schoolId,
        String schoolName,
        String subdomain,
        SchoolStatus schoolStatus,
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
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param school the school this row belongs to, or {@code null} when that school document has
     *               gone. A missing school leaves the four school fields null rather than failing
     *               the page — {@code schoolId} is still on the subscription itself, so the row
     *               remains traceable, which is what somebody investigating an orphaned
     *               subscription needs.
     * @param plan   the plan version this row points at, or {@code null} when that plan document
     *               has gone. Same reasoning: one unreadable row is worth more than a 500 for the
     *               other nineteen.
     * @param now    one instant for the whole page, so {@code periodEnded} cannot disagree
     *               between the first row and the last
     */
    public static PlatformSubscriptionRowResponse fromSubscription(SchoolSubscription subscription,
            School school, PlanDefinition plan, Instant now) {

        Instant periodEnd = subscription.getCurrentPeriodEnd();

        return new PlatformSubscriptionRowResponse(
                subscription.getId(),
                subscription.getSubscriptionNo(),
                subscription.getSchoolId(),
                school == null ? null : school.getSchoolName(),
                school == null ? null : school.getSubdomain(),
                school == null ? null : school.getStatus(),
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
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }
}
