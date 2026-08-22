package com.orbitastra.backend.models.plans;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.AuditedDocument;
import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Platform-level, versioned definition of one SaaS subscription plan.
 *
 * <p>This document is not school-owned and therefore extends AuditedDocument
 * directly. The immutable business identity is {@code planCode + planVersion}.
 * A published version should not be rewritten; material entitlement or pricing
 * changes create a new version.
 *
 * <p>SchoolSubscription links to this document through
 * {@code planDefinitionDocsId} and stores the selected {@code planVersion}.
 * {@code effectiveFrom/effectiveUntil} control when this plan version may be
 * sold, not the billing period of an existing subscription.
 */
@Document(collection = "plan_definitions")
@CompoundIndexes({
        @CompoundIndex(
                name = "plan_code_version_uniq",
                def = "{'planCode': 1, 'planVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "plan_status_effective_idx",
                def = "{'status': 1, 'effectiveFrom': -1, 'effectiveUntil': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDefinition extends AuditedDocument {

    // Example: "PREMIUM"
    @NotBlank
    private String planCode;

    // Example: 1
    @NotNull
    @Builder.Default
    private Integer planVersion = 1;

    // Example: "Premium"
    @NotBlank
    private String name;

    // Example: "Advanced ERP modules and AI capabilities for growing schools."
    private String description;

    // Example: PlanStatus.ACTIVE
    @NotNull
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;

    // Example: BillingCycle.YEARLY
    @NotNull
    private BillingCycle billingCycle;

    // Example: 49999.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal listPrice;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Example: 2000
    @NotNull
    private Long maxStudents;

    // Example: 250
    @NotNull
    private Long maxUsers;

    // Example: 2026-04-01T00:00:00Z
    private Instant effectiveFrom;

    // Example: 2027-03-31T23:59:59Z
    private Instant effectiveUntil;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean publiclyAvailable = false;

    // Example: [PlanFeature(featureCode="STUDENT_MANAGEMENT", enabled=true, usageLimit=2000, usageMetric="ACTIVE_STUDENTS")]
    @Builder.Default
    private List<PlanFeature> features = new ArrayList<>();
}
