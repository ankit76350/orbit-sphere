package com.orbitastra.backend.dto.plans.catalogue;

import java.util.List;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * A plan's whole feature list. What #3 returns, and what #10 will.
 *
 * <p>Its own record rather than {@link PlanResponse}, which reports only a {@code featureCount}.
 * That is right for a price change — a plan can carry a few dozen entitlements and returning them
 * all would bury the field that moved — and wrong here, where the list <i>is</i> what changed.
 *
 * <p>The rows are {@link PlanFeatureView}, shared with #10 — one shape for a feature however it
 * is asked for.
 *
 * <p>{@code status} is included even though this endpoint cannot change it. A caller replacing
 * features on a draft wants to see it is still a draft, because that is the thing that decides
 * whether they can do it again.
 */
public record PlanFeatureListResponse(
        String planCode,
        Integer planVersion,
        PlanStatus status,
        int featureCount,
        List<PlanFeatureView> features,
        String changeSummary) {

    public static PlanFeatureListResponse fromPlan(PlanDefinition plan, String changeSummary) {
        List<PlanFeatureView> features = plan.getFeatures() == null ? List.of()
                : plan.getFeatures().stream().map(PlanFeatureView::fromFeature).toList();

        return new PlanFeatureListResponse(
                plan.getPlanCode(),
                plan.getPlanVersion(),
                plan.getStatus(),
                features.size(),
                features,
                changeSummary);
    }
}
