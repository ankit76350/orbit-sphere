package com.orbitastra.backend.models.plans;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Current and historical contracted subscription periods for one school.
 *
 * <p>The inherited {@code schoolId} links to School.id.
 * {@code planDefinitionDocsId + planVersion} identify the selected immutable
 * PlanDefinition version. Contract price, billing cycle, and optional capacity
 * overrides are stored here because one school can negotiate terms different
 * from the public plan defaults.
 *
 * <p>Exactly one document per school may have {@code current = true}. The
 * current subscription is found by {@code schoolId + current}; subscription
 * fields are deliberately not duplicated in School.
 */
@Document(collection = "school_subscriptions")
@CompoundIndexes({
                @CompoundIndex(name = "school_subscription_no_uniq", def = "{'schoolId': 1, 'subscriptionNo': 1}", unique = true),
                @CompoundIndex(name = "school_current_subscription_uniq", def = "{'schoolId': 1, 'current': 1}", unique = true, partialFilter = "{'current': true}"),
                @CompoundIndex(name = "school_subscription_status_period_idx", def = "{'schoolId': 1, 'status': 1, 'currentPeriodEnd': 1}"),
                // Answers "who is on this plan version". Added 2026-09-03 for the plan version
                // history (#9), which asks it once per version — and for anything later that has
                // to know whether a version can be retired without stranding somebody. Without
                // it that question is a scan of every subscription in the platform, on a
                // collection that eventually holds one row per school.
                //
                // NOT school-scoped, unlike every other index here: the question is about a
                // plan, and a plan belongs to no school.
                @CompoundIndex(name = "subscription_plan_version_idx", def = "{'planDefinitionDocsId': 1, 'planVersion': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription extends SchoolBase {

        // Example: "SUB/2026/000001"
        @NotBlank
        private String subscriptionNo;

        // Links to PlanDefinition.id. Example: "67aa1202dc3f7d0012345678"
        @NotBlank
        private String planDefinitionDocsId;

        // Example: 1
        @NotNull
        private Integer planVersion;

        // Example: SubscriptionStatus.ACTIVE
        @NotNull
        private SubscriptionStatus status;

        // Example: BillingCycle.YEARLY
        @NotNull
        private BillingCycle billingCycle;

        // Example: 2026-04-01T00:00:00Z
        @NotNull
        private Instant currentPeriodStart;

        // Example: 2027-03-31T23:59:59Z
        @NotNull
        private Instant currentPeriodEnd;

        // Example: true
        @NotNull
        @Builder.Default
        private Boolean autoRenew = true;

        // Example: 45000.00
        @NotNull
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal contractedPrice;

        // Example: "INR"
        @NotBlank
        private String currencyCode;

        // Optional per-school student limit; null uses PlanDefinition.maxStudents. Example: 2500
        private Long maxStudentsOverride;

        // Optional per-school user limit; null uses PlanDefinition.maxUsers. Example: 300
        private Long maxUsersOverride;

        // Example: true
        @NotNull
        @Builder.Default
        private Boolean current = true;

        //!
        // Example: "customer_Qx7B2mR9"
        private String billingCustomerReference;

        //!
        // Example: 2027-02-15T10:30:00Z
        private Instant cancelledAt;

        //!
        // Example: "School requested cancellation at the end of the billing period."
        private String cancellationReason;
}
