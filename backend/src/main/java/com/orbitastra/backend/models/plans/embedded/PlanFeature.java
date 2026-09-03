package com.orbitastra.backend.models.plans.embedded;

import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;
import com.orbitastra.backend.models.plans.enums.UsageMetric;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One feature entitlement embedded inside a PlanDefinition version.
 *
 * <p>It has no collection identity. Read a row as a sentence: <i>this plan includes X, up to N of
 * Y, and does Z when the school goes past it.</i>
 *
 * <p><b>{@code featureCode} became an enum on 2026-09-03.</b> It was a free string, which
 * accepted {@code STUDNET_MANAGEMENT} without complaint — the plan looked right on every screen
 * while the entitlement service, asking for {@code STUDENT_MANAGEMENT}, found nothing and locked
 * the school out of what they had paid for. A feature code points at behaviour in this codebase
 * rather than at anything a user invents, so the set of them is closed and belongs in
 * {@link FeatureCode}.
 *
 * <p>{@code usageLimit} is optional; null means no numeric limit. Where a limit exists,
 * {@code usageMetric} says what the number counts.
 *
 * <p><b>{@code usageMetric} is copied from the feature, never sent by a caller.</b> Each
 * {@link FeatureCode} declares what it is measured in, so "student management limited to 2000
 * gigabytes" cannot be written down. It is <b>stored</b> rather than derived on read, because a
 * published plan version is immutable: if the enum's metric for a feature were ever changed, a
 * plan sold last year must keep meaning what it meant when it was sold.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeature {

    // Which capability the plan grants. Example: FeatureCode.STUDENT_MANAGEMENT
    @NotNull
    private FeatureCode featureCode;

    // Whether the plan includes it at all. False deliberately lists it as not included, so a
    // comparison table can show it with a cross rather than leaving it out. Example: true
    @NotNull
    @Builder.Default
    private Boolean enabled = true;

    // How much is included. Null means no numeric limit. Example: 2000
    private Long usageLimit;

    // What usageLimit counts, copied from the feature when the plan was written and frozen
    // there. Null whenever usageLimit is null. Example: UsageMetric.ACTIVE_STUDENTS
    private UsageMetric usageMetric;

    // What happens past the limit. Example: OveragePolicy.BLOCK
    @NotNull
    @Builder.Default
    private OveragePolicy overagePolicy = OveragePolicy.BLOCK;
}
