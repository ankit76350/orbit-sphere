package com.orbitastra.backend.dto.plans.catalogue;

import java.util.List;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;
import com.orbitastra.backend.models.plans.enums.UsageMetric;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * A plan's whole feature list. What #3 returns, and what #10 will.
 *
 * <p>Its own record rather than {@link PlanResponse}, which reports only a {@code featureCount}.
 * That is right for a price change — a plan can carry a few dozen entitlements and returning them
 * all would bury the field that moved — and wrong here, where the list <i>is</i> what changed.
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
        List<FeatureView> features,
        String changeSummary) {

    /**
     * One entitlement as the API returns it.
     *
     * <p>{@code label} and {@code description} come from the {@link FeatureCode} enum, which is
     * the only place they are written. A pricing page, a plan comparison and the "your plan does
     * not include this" message then all say the same words, rather than three screens each
     * inventing their own wording for the same feature.
     */
    public record FeatureView(
            FeatureCode featureCode,
            String label,
            String description,
            Boolean enabled,
            Long usageLimit,
            UsageMetric usageMetric,
            OveragePolicy overagePolicy) {
    }

    public static PlanFeatureListResponse fromPlan(PlanDefinition plan, String changeSummary) {
        List<FeatureView> features = plan.getFeatures() == null ? List.of()
                : plan.getFeatures().stream()
                        .map(one -> new FeatureView(
                                one.getFeatureCode(),
                                one.getFeatureCode().getLabel(),
                                one.getFeatureCode().getDescription(),
                                one.getEnabled(),
                                one.getUsageLimit(),
                                one.getUsageMetric(),
                                one.getOveragePolicy()))
                        .toList();

        return new PlanFeatureListResponse(
                plan.getPlanCode(),
                plan.getPlanVersion(),
                plan.getStatus(),
                features.size(),
                features,
                changeSummary);
    }
}
