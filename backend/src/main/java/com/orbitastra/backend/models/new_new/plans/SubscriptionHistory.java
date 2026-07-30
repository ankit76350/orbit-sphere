package com.orbitastra.backend.models.new_new.plans;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.new_new.plans.enums.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "subscription_history")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_subscription_sequence_uniq",
                def = "{'schoolId': 1, 'schoolSubscriptionDocsId': 1, 'sequenceNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_subscription_event_time_idx",
                def = "{'schoolId': 1, 'schoolSubscriptionDocsId': 1, 'effectiveAt': -1}"),
        @CompoundIndex(
                name = "school_subscription_source_event_uniq",
                def = "{'schoolId': 1, 'source': 1, 'sourceEventId': 1}",
                unique = true,
                partialFilter = "{'sourceEventId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionHistory extends SchoolBase {
        // SubscriptionHistory stores every change made to that subscription:

    // Example: "67aa1a44dc3f7d0011223344"
    private String schoolSubscriptionDocsId;

    // Example: SubscriptionEventType.PLAN_CHANGED
    private SubscriptionEventType eventType;

    // Example: SubscriptionStatus.ACTIVE
    private SubscriptionStatus previousStatus;

    // Example: SubscriptionStatus.ACTIVE
    private SubscriptionStatus newStatus;

    // Example: "67aa1202dc3f7d0012345678"
    private String previousPlanDefinitionDocsId;


    // Example: "67aa21bedc3f7d0055667788"
    private String newPlanDefinitionDocsId;

    // Example: "ADMIN_PORTAL"
    private String source;

    // Example: "billing_event_00004519"
    private String sourceEventId;

    // Example: "School upgraded from Premium to Enterprise."
    private String reason;

    // Example: "67aa2b73dc3f7d0099887766"
    private String performedByDocsId;

    // Example: 2026-08-01T00:00:00Z
    private Instant effectiveAt;
}
