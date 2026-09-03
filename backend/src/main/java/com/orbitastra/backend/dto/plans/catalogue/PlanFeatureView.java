package com.orbitastra.backend.dto.plans.catalogue;

import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;
import com.orbitastra.backend.models.plans.enums.UsageMetric;

/**
 * One entitlement as the API returns it.
 *
 * <p>Its own file because two endpoints return features — #3 after setting them and #10 when
 * showing a whole version — and two records with the same fields is two things to keep in step.
 * A client that can read one can read the other.
 *
 * <p>{@code label} and {@code description} come from {@link FeatureCode}, the only place they are
 * written. A pricing page, a plan comparison and a "your plan does not include this" message
 * then all say the same words about the same feature, instead of three screens inventing their
 * own wording.
 */
public record PlanFeatureView(
        FeatureCode featureCode,
        String label,
        String description,
        Boolean enabled,
        Long usageLimit,
        UsageMetric usageMetric,
        OveragePolicy overagePolicy) {

    public static PlanFeatureView fromFeature(PlanFeature feature) {
        return new PlanFeatureView(
                feature.getFeatureCode(),
                feature.getFeatureCode().getLabel(),
                feature.getFeatureCode().getDescription(),
                feature.getEnabled(),
                feature.getUsageLimit(),
                feature.getUsageMetric(),
                feature.getOveragePolicy());
    }
}
