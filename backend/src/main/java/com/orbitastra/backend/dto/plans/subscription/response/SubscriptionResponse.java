package com.orbitastra.backend.dto.plans.subscription.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * What a school is subscribed to. Shared by the subscription writes.
 *
 * <p>Carries the <b>plan's code, version and name alongside the link to it</b>, because a
 * subscription that reports only {@code planDefinitionDocsId} is unreadable: nobody can tell
 * from a Mongo id whether the school is on Premium v1 or Standard v3, and every caller would
 * have to fetch the plan to find out.
 *
 * <p>{@code contractedPrice} against {@code planListPrice} is the other pair worth seeing
 * together — it is the only way to notice a school is on a discount, and the discount is the
 * thing somebody will ask about.
 */
public record SubscriptionResponse(
        String subscriptionId,
        String subscriptionNo,
        String schoolId,
        String planDefinitionDocsId,
        String planCode,
        Integer planVersion,
        String planName,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Boolean autoRenew,
        BigDecimal contractedPrice,
        BigDecimal planListPrice,
        String currencyCode,
        Long maxStudents,
        Long maxUsers,
        boolean hasLimitOverrides,
        Boolean current,
        String nextStep) {

    /**
     * @param plan the version the subscription points at, so the response can name it and show
     *             the limits actually in force
     */
    public static SubscriptionResponse fromSubscription(SchoolSubscription subscription,
            PlanDefinition plan, String nextStep) {

        // The ceiling in force is the school's override where there is one, and the plan's
        // otherwise. Reporting the raw override would leave every caller doing this themselves,
        // and a null would read as "no limit" rather than "the plan's limit".
        Long students = subscription.getMaxStudentsOverride() != null
                ? subscription.getMaxStudentsOverride()
                : plan.getMaxStudents();
        Long users = subscription.getMaxUsersOverride() != null
                ? subscription.getMaxUsersOverride()
                : plan.getMaxUsers();

        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getSubscriptionNo(),
                subscription.getSchoolId(),
                subscription.getPlanDefinitionDocsId(),
                plan.getPlanCode(),
                subscription.getPlanVersion(),
                plan.getName(),
                subscription.getStatus(),
                subscription.getBillingCycle(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getAutoRenew(),
                subscription.getContractedPrice(),
                plan.getListPrice(),
                subscription.getCurrencyCode(),
                students,
                users,
                negotiatedCapacity(subscription, plan),
                subscription.getCurrent(),
                nextStep);
    }

    /**
     * True when this school's capacity is not the plan's.
     *
     * <p><b>Not simply "an override is set".</b> #13 writes the capacity onto every subscription,
     * copying the plan's figures when the caller named none, so a null check would answer true
     * for every subscription ever sold and the flag would tell a reader nothing. What is worth
     * flagging is a school whose ceiling was negotiated away from what its plan lists.
     */
    private static boolean negotiatedCapacity(SchoolSubscription subscription,
            PlanDefinition plan) {

        return awayFromPlan(subscription.getMaxStudentsOverride(), plan.getMaxStudents())
                || awayFromPlan(subscription.getMaxUsersOverride(), plan.getMaxUsers());
    }

    /**
     * One ceiling against the plan's.
     *
     * <p><b>Null is not a difference.</b> It means "follow the plan", so the limit in force *is*
     * the plan's — which is exactly the state #14 leaves behind when an override is removed, and
     * reporting that as negotiated would put the badge on the one subscription that has no
     * negotiated limit at all.
     */
    private static boolean awayFromPlan(Long override, Long planLimit) {
        return override != null && !override.equals(planLimit);
    }
}
