package com.orbitastra.backend.dto.plans.catalogue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * One plan version in full, features included. Endpoint #10.
 *
 * <p><b>Not {@link PlanResponse}, which the writes use.</b> That record carries a
 * {@code nextStep} — a sentence about what just happened and what to do next — and a read did not
 * make anything happen. It also reports only a {@code featureCount}, which is right for a price
 * change and wrong here: this endpoint exists to show the features.
 *
 * <p>So this is the whole document as a caller should see it, and nothing about a transition.
 *
 * <p>{@code schoolsOnThisVersion} is not in the endpoint's field list, and is here because it is
 * the question somebody opening one version actually has: <i>can this be retired, or is
 * somebody on it?</i> #9 reports it per version for the same reason.
 */
public record PlanDetailResponse(
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
        Boolean publiclyAvailable,
        boolean sellable,
        Instant effectiveFrom,
        Instant effectiveUntil,
        int featureCount,
        List<PlanFeatureView> features,
        long schoolsOnThisVersion,
        Instant createdAt,
        Instant updatedAt,
        String note) {

    public static PlanDetailResponse fromPlan(PlanDefinition plan, long schoolsOnThisVersion,
            String note) {

        List<PlanFeatureView> features = plan.getFeatures() == null ? List.of()
                : plan.getFeatures().stream().map(PlanFeatureView::fromFeature).toList();

        return new PlanDetailResponse(
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
                plan.getPubliclyAvailable(),
                PlanResponse.isSellable(plan),
                plan.getEffectiveFrom(),
                plan.getEffectiveUntil(),
                features.size(),
                features,
                schoolsOnThisVersion,
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                note);
    }
}
