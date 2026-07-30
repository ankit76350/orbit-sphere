package com.orbitastra.backend.models.new_new.plans.embedded;

import com.orbitastra.backend.models.new_new.plans.enums.OveragePolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeature {

    // Example: "STUDENT_MANAGEMENT"
    private String featureCode;

    // Example: true
    @Builder.Default
    private Boolean enabled = true;

    // Example: 2000
    private Long usageLimit;

    // Example: "ACTIVE_STUDENTS"
    private String usageMetric;

    // Example: OveragePolicy.BLOCK
    @Builder.Default
    private OveragePolicy overagePolicy = OveragePolicy.BLOCK;
}
