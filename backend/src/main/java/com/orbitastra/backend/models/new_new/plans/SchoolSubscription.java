package com.orbitastra.backend.models.new_new.plans;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.plans.enums.BillingCycle;
import com.orbitastra.backend.models.new_new.plans.enums.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "school_subscriptions")
@CompoundIndexes({
                @CompoundIndex(name = "school_subscription_no_uniq", def = "{'schoolId': 1, 'subscriptionNo': 1}", unique = true),
                @CompoundIndex(name = "school_current_subscription_uniq", def = "{'schoolId': 1, 'current': 1}", unique = true, partialFilter = "{'current': true}"),
                @CompoundIndex(name = "school_subscription_status_period_idx", def = "{'schoolId': 1, 'status': 1, 'currentPeriodEnd': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription extends SchoolBase {
        // SchoolSubscription stores the current subscription state:

        // Example: "SUB/2026/000001"
        private String subscriptionNo;

        // Example: "67aa1202dc3f7d0012345678"
        private String planDefinitionDocsId;

        // Example: 1
        private Integer planVersion;

        // Example: SubscriptionStatus.ACTIVE
        private SubscriptionStatus status;

        // Example: BillingCycle.YEARLY
        private BillingCycle billingCycle;

        // Example: 2026-04-01T00:00:00Z
        private Instant currentPeriodStart;

        // Example: 2027-03-31T23:59:59Z
        private Instant currentPeriodEnd;

        // Example: true
        @Builder.Default
        private Boolean autoRenew = true;

        // Example: 45000.00
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal contractedPrice;

        // Example: "INR"
        private String currencyCode;

        // Example: "customer_Qx7B2mR9"
        private String billingCustomerReference;

        // Example: "67aa15d9dc3f7d0098765432"
        // We will later decide that we need this or not when we will deal with this
        // backend/src/main/java/com/orbitastra/backend/models/new_new/plans/SchoolSubscription.java
        // package com.orbitastra.backend.models.undone.a_new.legal;
        private String contractDocsId;

        // Example: 2027-02-15T10:30:00Z
        private Instant cancelledAt;

        // Example: "School requested cancellation at the end of the billing period."
        private String cancellationReason;

        // Example: true
        @Builder.Default
        private Boolean current = true;
}
