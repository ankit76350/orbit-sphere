package com.orbitastra.backend.dto.plans.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureView;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * What one school is on right now, in full. Endpoint #27.
 *
 * <p><b>The features are included here, unlike on the plan list.</b> A list shows a count because
 * a few dozen feature rows on every row of a page is noise. This is one school, and "what has
 * this school actually paid for" is the question it exists to answer, so the rows are the answer.
 *
 * <h2>It works out three things the caller would otherwise get wrong</h2>
 *
 * <p><b>{@code daysRemaining}</b> — how long is left in the period. Counting days between two
 * instants in the browser is where time zones go wrong, and every screen showing this would do
 * the same sum.
 *
 * <p><b>{@code periodEnded}</b> — the period's end has passed while the status still says the
 * subscription is live. That is a real state today, not a hypothetical: nothing renews a
 * subscription or marks one expired yet (#21 and #26 are not built), so a period simply lapses
 * and the status stays as it was. A screen reading `status` alone would report a school as paying
 * when its period ran out months ago.
 *
 * <p><b>{@code planRetired}</b> — the plan this school is on has been taken off the menu. That is
 * allowed and normal: retiring stops new sales and changes nothing for schools already on it. But
 * it is worth saying, because it is the reason the plan cannot be found on the public list.
 *
 * <p>{@code contractedPrice} and {@code planListPrice} are both here for the same reason they are
 * on every other subscription response: the gap between them is a discount, and a discount is the
 * thing somebody rings up about.
 */
public record SubscriptionDetailResponse(

        String subscriptionId,
        String subscriptionNo,
        String schoolId,

        // the plan they are on
        String planDefinitionDocsId,
        String planCode,
        Integer planVersion,
        String planName,
        PlanStatus planStatus,
        boolean planRetired,

        // where the subscription is in its life
        SubscriptionStatus status,
        BillingCycle billingCycle,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Long daysRemaining,
        boolean periodEnded,
        Boolean autoRenew,
        Boolean current,

        // the money
        BigDecimal contractedPrice,
        BigDecimal planListPrice,
        String currencyCode,
        boolean hasDiscount,

        // the ceilings actually in force
        Long maxStudents,
        Long maxUsers,
        Long maxStudentsOverride,
        Long maxUsersOverride,
        boolean hasLimitOverrides,

        // what they are entitled to
        int featureCount,
        List<PlanFeatureView> features,

        // the ending, where there is one
        Instant cancelledAt,
        String cancellationReason,

        String billingCustomerReference,
        String note) {

    /**
     * @param plan the version the subscription points at, so the response can name it and list
     *             the features and limits actually in force
     * @param note a plain sentence about anything odd — a lapsed period, a retired plan
     */
    public static SubscriptionDetailResponse fromSubscription(SchoolSubscription subscription,
            PlanDefinition plan, String note) {

        List<PlanFeatureView> features = plan.getFeatures() == null
                ? List.of()
                : plan.getFeatures().stream().map(PlanFeatureView::fromFeature).toList();

        // The ceiling in force is the school's override where there is one, and the plan's
        // otherwise. Both are reported: the first is what applies, the second says whether it
        // was negotiated.
        Long students = subscription.getMaxStudentsOverride() != null
                ? subscription.getMaxStudentsOverride()
                : plan.getMaxStudents();
        Long users = subscription.getMaxUsersOverride() != null
                ? subscription.getMaxUsersOverride()
                : plan.getMaxUsers();

        Instant periodEnd = subscription.getCurrentPeriodEnd();
        Instant now = Instant.now();
        boolean ended = periodEnd != null && !periodEnd.isAfter(now);
        Long daysLeft = periodEnd == null ? null : ChronoUnit.DAYS.between(now, periodEnd);

        BigDecimal contracted = subscription.getContractedPrice();
        BigDecimal list = plan.getListPrice();
        boolean discounted = contracted != null && list != null
                && contracted.compareTo(list) < 0;

        return new SubscriptionDetailResponse(
                subscription.getId(),
                subscription.getSubscriptionNo(),
                subscription.getSchoolId(),
                subscription.getPlanDefinitionDocsId(),
                plan.getPlanCode(),
                subscription.getPlanVersion(),
                plan.getName(),
                plan.getStatus(),
                plan.getStatus() == PlanStatus.RETIRED,
                subscription.getStatus(),
                subscription.getBillingCycle(),
                subscription.getCurrentPeriodStart(),
                periodEnd,
                daysLeft,
                ended,
                subscription.getAutoRenew(),
                subscription.getCurrent(),
                contracted,
                list,
                subscription.getCurrencyCode(),
                discounted,
                students,
                users,
                subscription.getMaxStudentsOverride(),
                subscription.getMaxUsersOverride(),
                subscription.getMaxStudentsOverride() != null
                        || subscription.getMaxUsersOverride() != null,
                features.size(),
                features,
                subscription.getCancelledAt(),
                subscription.getCancellationReason(),
                subscription.getBillingCustomerReference(),
                note);
    }
}
