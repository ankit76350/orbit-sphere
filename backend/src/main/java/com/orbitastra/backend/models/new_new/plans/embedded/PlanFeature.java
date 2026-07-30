package com.orbitastra.backend.models.new_new.plans.embedded;

import com.orbitastra.backend.models.new_new.plans.enums.OveragePolicy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One feature entitlement embedded inside a PlanDefinition version.
 *
 * <p>It has no collection identity. {@code usageLimit} is optional; null means
 * no numeric limit is defined for this feature. When a limit exists,
 * {@code usageMetric} identifies the counter measured by the entitlement
 * service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeature {

    // Example: "STUDENT_MANAGEMENT"
    @NotBlank
    private String featureCode;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean enabled = true;

    // Example: 2000
    private Long usageLimit;

    // Example: "ACTIVE_STUDENTS"
    private String usageMetric;

    // Example: OveragePolicy.BLOCK
    @NotNull
    @Builder.Default
    private OveragePolicy overagePolicy = OveragePolicy.BLOCK;
}
