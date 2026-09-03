package com.orbitastra.backend.dto.plans.catalogue;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * One plan version as it now stands. Shared by every write in the catalogue group.
 *
 * <p>One record rather than one per endpoint. Create, edit, publish, retire and change
 * availability all leave the caller asking the same thing — what does this plan look like now —
 * and {@code nextStep} is what distinguishes them.
 *
 * <p>{@code featureCount} rather than the features themselves. A plan can carry a few dozen
 * entitlements, and returning them all on a price change would bury the field that changed. The
 * feature list has its own endpoints (#3 to set, #10 to read).
 *
 * <p>{@code sellable} is <b>derived here, never stored</b>. Whether a plan can be bought today
 * is three separate facts — it is published, it is public, and today is inside its selling
 * window — and every screen that has to answer it would otherwise combine them itself, slightly
 * differently each time.
 */
public record PlanResponse(
        String planId,
        String planCode,
        Integer planVersion,
        String name,
        String description,
        PlanStatus status,
        BillingCycle billingCycle,
        BigDecimal listPrice,
        String currencyCode,
        Long maxStudents,
        Long maxUsers,
        Instant effectiveFrom,
        Instant effectiveUntil,
        Boolean publiclyAvailable,
        int featureCount,
        boolean sellable,
        String nextStep) {

    /**
     * Whether a school could buy this plan right now.
     *
     * <p><b>Three facts, and all three have to hold:</b> the plan is published, it is on the
     * public list, and today is inside its selling window. Each is set by a different endpoint —
     * #4, #7, and #1 or #2 — which is exactly why this is computed in one place. Two screens
     * combining the same three conditions slightly differently is how a plan comes to look
     * buyable on one page and not on another.
     *
     * <p>Public and static because the catalogue list reports it too; see
     * {@link PlanSummaryResponse}.
     */
    public static boolean isSellable(PlanDefinition plan) {
        Instant now = Instant.now();
        boolean started = plan.getEffectiveFrom() == null || !now.isBefore(plan.getEffectiveFrom());
        boolean notEnded = plan.getEffectiveUntil() == null || !now.isAfter(plan.getEffectiveUntil());

        return plan.getStatus() == PlanStatus.ACTIVE
                && Boolean.TRUE.equals(plan.getPubliclyAvailable())
                && started
                && notEnded;
    }

    public static PlanResponse fromPlan(PlanDefinition plan, String nextStep) {
        return new PlanResponse(
                plan.getId(),
                plan.getPlanCode(),
                plan.getPlanVersion(),
                plan.getName(),
                plan.getDescription(),
                plan.getStatus(),
                plan.getBillingCycle(),
                plan.getListPrice(),
                plan.getCurrencyCode(),
                plan.getMaxStudents(),
                plan.getMaxUsers(),
                plan.getEffectiveFrom(),
                plan.getEffectiveUntil(),
                plan.getPubliclyAvailable(),
                plan.getFeatures() == null ? 0 : plan.getFeatures().size(),
                isSellable(plan),
                nextStep);
    }
}
