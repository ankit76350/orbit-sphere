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

    public static PlanResponse fromPlan(PlanDefinition plan, String nextStep) {
        Instant now = Instant.now();
        boolean started = plan.getEffectiveFrom() == null || !now.isBefore(plan.getEffectiveFrom());
        boolean notEnded = plan.getEffectiveUntil() == null || !now.isAfter(plan.getEffectiveUntil());

        boolean sellable = plan.getStatus() == PlanStatus.ACTIVE
                && Boolean.TRUE.equals(plan.getPubliclyAvailable())
                && started
                && notEnded;

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
                sellable,
                nextStep);
    }
}
