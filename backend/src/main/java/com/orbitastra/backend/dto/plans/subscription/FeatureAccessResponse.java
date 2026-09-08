package com.orbitastra.backend.dto.plans.subscription;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.models.plans.enums.UsageMetric;

/**
 * What this school is allowed to use. Endpoint #34.
 *
 * <p><b>This is the one every other module asks.</b> Nothing else may read
 * {@code plan_definitions.features} to decide whether a school can use something, because the
 * moment two places work that out they disagree — and they disagree quietly, in the direction of
 * letting somebody use what they have not paid for.
 *
 * <h2>Read {@code allowed}, not {@code includedInPlan}</h2>
 *
 * <p>The two are different and the difference is the whole reason this endpoint exists.
 * {@code includedInPlan} is what the plan says. {@code allowed} is whether the school may use it
 * <b>right now</b> — the plan saying yes, and the subscription being in a state that grants
 * anything at all. A cancelled subscription on a plan full of features grants none of them.
 *
 * <p>Both are returned because a screen wants to say "your plan includes Transport, but your
 * subscription has lapsed" rather than simply hiding it. A gate wants {@code allowed} and
 * nothing else.
 *
 * <p><b>{@code allowed} is false on every feature when the subscription grants nothing</b>, and
 * that is deliberate rather than left to the caller to remember. A caller that reads the feature
 * rows and forgets the top-level {@code active} flag still gets the right answer, because the
 * flag has already been folded in here. Making the safe reading the easy one is the only way a
 * rule like this survives contact with a dozen call sites.
 *
 * <h2>What it does not do</h2>
 *
 * <p><b>No usage counts.</b> This says what the ceiling is, not how much of it is gone —
 * counting students and user accounts is #35, and it is separate on purpose: a gate check runs on
 * every request that touches a feature, and counting rows on each one would be the most
 * expensive query in the product. Callers hold their own count and compare it to the limit here.
 */
public record EntitlementsResponse(

        /** Whether the subscription grants anything at all right now. */
        boolean active,

        /** Why not, when {@code active} is false. Null when it is true. */
        String reason,

        String subscriptionNo,
        SubscriptionStatus status,
        String planName,
        Integer planVersion,
        Instant currentPeriodEnd,

        /** The ceilings in force — the school's negotiated limit where it has one. */
        Long maxStudents,
        Long maxUsers,

        int featureCount,
        List<Entitlement> features) {

    /**
     * One feature, and what this school may do with it.
     *
     * @param includedInPlan what the plan says
     * @param allowed        whether it may be used right now — the plan saying yes AND the
     *                       subscription granting anything. This is the one a gate reads.
     * @param usageLimit     the ceiling, or null for no numeric ceiling
     * @param usageMetric    what the ceiling counts. Null whenever usageLimit is null
     * @param overagePolicy  what to do when the ceiling is passed
     */
    public record Entitlement(
            FeatureCode featureCode,
            String label,
            boolean includedInPlan,
            boolean allowed,
            Long usageLimit,
            UsageMetric usageMetric,
            OveragePolicy overagePolicy) {
    }
}
