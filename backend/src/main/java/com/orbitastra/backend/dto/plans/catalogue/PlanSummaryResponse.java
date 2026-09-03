package com.orbitastra.backend.dto.plans.catalogue;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * One plan version as it appears in the catalogue list. Endpoint #8.
 *
 * <p>A summary. A list is read to find a plan, not to work on one, so the features themselves are
 * a count here and #10 returns them in full once somebody has picked a version.
 *
 * <p>{@code sellable} is the field the list exists for: an operator scanning the catalogue is
 * usually asking which of these a school can actually buy right now. It comes from
 * {@link PlanResponse#isSellable} so the list and the single-plan responses can never disagree
 * about it.
 */
public record PlanSummaryResponse(
        String planId,
        String planCode,
        Integer planVersion,
        String name,
        PlanStatus status,
        BillingCycle billingCycle,
        BigDecimal listPrice,
        String currencyCode,
        Long maxStudents,
        Long maxUsers,
        Boolean publiclyAvailable,
        boolean sellable,
        int featureCount,
        Instant effectiveFrom,
        Instant effectiveUntil,
        Instant createdAt) {

    public static PlanSummaryResponse fromPlan(PlanDefinition plan) {
        return new PlanSummaryResponse(
                plan.getId(),
                plan.getPlanCode(),
                plan.getPlanVersion(),
                plan.getName(),
                plan.getStatus(),
                plan.getBillingCycle(),
                plan.getListPrice(),
                plan.getCurrencyCode(),
                plan.getMaxStudents(),
                plan.getMaxUsers(),
                plan.getPubliclyAvailable(),
                PlanResponse.isSellable(plan),
                plan.getFeatures() == null ? 0 : plan.getFeatures().size(),
                plan.getEffectiveFrom(),
                plan.getEffectiveUntil(),
                plan.getCreatedAt());
    }
}
