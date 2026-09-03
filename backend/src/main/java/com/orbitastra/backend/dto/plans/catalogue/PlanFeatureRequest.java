package com.orbitastra.backend.dto.plans.catalogue;

import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;

import jakarta.validation.constraints.NotNull;

/**
 * One entitlement in a plan's feature list. Endpoint #3 takes a list of these.
 *
 * <p>A feature says what the plan lets a school do, and — when there is a number involved — how
 * much of it and what happens when they go over.
 *
 * <p><b>{@code usageMetric} is not here.</b> It used to be, and it let a caller write "student
 * management, limited to 2000 gigabytes". Each {@link FeatureCode} now declares what it is
 * measured in, so the metric is copied from the feature and the mismatch cannot be expressed.
 *
 * <p><b>{@code enabled: false} is how a feature is switched off</b>, not a {@code usageLimit} of
 * zero. Zero reads as "allowed, but never" — the same outcome by a route that leaves the feature
 * looking available on every screen that lists what the plan includes.
 */
public record PlanFeatureRequest(

        /**
         * Which capability. Example: FeatureCode.STUDENT_MANAGEMENT
         *
         * <p>One of a fixed list. An unknown value is refused with the accepted ones named,
         * rather than stored as a code nothing will ever match.
         */
        @NotNull FeatureCode featureCode,

        /**
         * Whether the plan includes this at all. Defaults to true when omitted.
         *
         * <p>Boxed, not a primitive: an omitted {@code boolean} cannot be told from
         * {@code false}, so the default could never be applied.
         */
        Boolean enabled,

        /**
         * How much is included. Omit for no numeric limit. Example: 2000
         *
         * <p>Only allowed on a feature that has something to count. A limit on "attendance"
         * is refused — it is included or it is not.
         */
        Long usageLimit,

        /** What happens past the limit. Defaults to BLOCK. Example: OveragePolicy.WARN */
        OveragePolicy overagePolicy) {

    /** The metric comes from the feature itself, and only when there is a limit to measure. */
    public PlanFeature toFeature() {
        return PlanFeature.builder()
                .featureCode(featureCode)
                .enabled(enabled == null ? Boolean.TRUE : enabled)
                .usageLimit(usageLimit)
                .usageMetric(usageLimit == null ? null : featureCode.getUsageMetric())
                .overagePolicy(overagePolicy == null ? OveragePolicy.BLOCK : overagePolicy)
                .build();
    }
}
