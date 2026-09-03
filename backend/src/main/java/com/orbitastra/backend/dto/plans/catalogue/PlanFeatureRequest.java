package com.orbitastra.backend.dto.plans.catalogue;

import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One entitlement in a plan's feature list. Endpoint #3 takes an array of these.
 *
 * <p>A feature says what the plan lets a school do, and — when there is a number involved — how
 * much of it and what happens when they go over.
 *
 * <p><b>{@code usageLimit} and {@code usageMetric} belong together.</b> A limit of 2000 with no
 * metric is a number nothing can measure: the entitlement service would not know whether 2000
 * counts students, staff, SMS messages or gigabytes. The model says as much, and #3 refuses the
 * pair half-filled rather than storing a limit nobody can enforce.
 *
 * <p>{@code enabled: false} is how a feature is switched off, not a {@code usageLimit} of zero.
 * Zero reads as "allowed, but never" — which is the same outcome by a route that leaves the
 * feature looking available on every screen that lists it.
 */
public record PlanFeatureRequest(

        /** Uppercased on the way in. Example: "STUDENT_MANAGEMENT" */
        @NotBlank @Size(max = 60) String featureCode,

        /**
         * Whether the plan includes this at all. Defaults to true when omitted.
         *
         * <p>Boxed, not a primitive: an omitted {@code boolean} cannot be told from
         * {@code false}, so the default could never be applied.
         */
        Boolean enabled,

        /** How much is included. Omit for no numeric limit. Example: 2000 */
        Long usageLimit,

        /** Which counter the limit is measured in. Required with a limit. Example: "ACTIVE_STUDENTS" */
        @Size(max = 60) String usageMetric,

        /** What happens past the limit. Defaults to BLOCK. Example: OveragePolicy.WARN */
        OveragePolicy overagePolicy) {

    /** With the code already normalized, since the service is the only thing that can do that. */
    public PlanFeature toFeature(String normalizedCode) {
        return PlanFeature.builder()
                .featureCode(normalizedCode)
                .enabled(enabled == null ? Boolean.TRUE : enabled)
                .usageLimit(usageLimit)
                .usageMetric(usageMetric == null || usageMetric.isBlank()
                        ? null
                        : usageMetric.trim().toUpperCase())
                .overagePolicy(overagePolicy == null ? OveragePolicy.BLOCK : overagePolicy)
                .build();
    }
}
