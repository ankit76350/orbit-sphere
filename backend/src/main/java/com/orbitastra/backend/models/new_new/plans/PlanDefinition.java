package com.orbitastra.backend.models.new_new.plans;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.AuditedDocument;
import com.orbitastra.backend.models.new_new.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.new_new.plans.enums.BillingCycle;
import com.orbitastra.backend.models.new_new.plans.enums.PlanStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    private String planCode;

    // Example: 1
    @Builder.Default
    private Integer planVersion = 1;

    // Example: "Premium"
    private String name;

    // Example: "Advanced ERP modules and AI capabilities for growing schools."
    private String description;

    // Example: PlanStatus.ACTIVE
    private PlanStatus status;

    // Example: BillingCycle.YEARLY
    private BillingCycle billingCycle;

    // Example: 49999.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal listPrice;

    // Example: "INR"
    private String currencyCode;

    // Example: 2000
    private Long maxStudents;

    // Example: 250
    private Long maxUsers;

    // Example: 2026-04-01T00:00:00Z
    private Instant effectiveFrom;

    // Example: 2027-03-31T23:59:59Z
    private Instant effectiveUntil;

    // Example: true
    @Builder.Default
    private Boolean publiclyAvailable = false;

    // Example: [PlanFeature(featureCode="STUDENT_MANAGEMENT", enabled=true, usageLimit=2000, usageMetric="ACTIVE_STUDENTS")]
    @Builder.Default
    private List<PlanFeature> features = new ArrayList<>();
}
