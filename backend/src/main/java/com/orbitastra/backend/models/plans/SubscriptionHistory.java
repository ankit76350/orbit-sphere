package com.orbitastra.backend.models.new_new.plans;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.new_new.plans.enums.SubscriptionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Append-only audit history for changes to one SchoolSubscription.
 *
 * <p>{@code schoolSubscriptionDocsId} links to the subscription.
 * {@code previousStatus/newStatus} and previous/new plan ids capture the
 * transition without rewriting earlier events. {@code sourceEventId} is an
 * optional idempotency key supplied by an external billing provider or
 * integration; the partial unique index prevents processing the same event
 * twice.
 *
 * <p>History entries must not be updated or deleted during normal operation.
 */
@Document(collection = "subscription_history")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_subscription_event_time_idx",
                def = "{'schoolId': 1, 'schoolSubscriptionDocsId': 1, 'effectiveAt': -1, 'createdAt': -1}"),
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

    // Links to SchoolSubscription.id. Example: "67aa1a44dc3f7d0011223344"
    @NotBlank
    private String schoolSubscriptionDocsId;

    // Example: SubscriptionEventType.PLAN_CHANGED
    @NotNull
    private SubscriptionEventType eventType;

    // Example: SubscriptionStatus.ACTIVE
    private SubscriptionStatus previousStatus;

    // Example: SubscriptionStatus.ACTIVE
    @NotNull
    private SubscriptionStatus newStatus;

    // Example: "67aa1202dc3f7d0012345678"
    private String previousPlanDefinitionDocsId;


    // Example: "67aa21bedc3f7d0055667788"
    private String newPlanDefinitionDocsId;

    // Example: "ADMIN_PORTAL"
    @NotBlank
    private String source;

    // Example: "billing_event_00004519"
    private String sourceEventId;

    // Example: "School upgraded from Premium to Enterprise."
    private String reason;

    // Links to the acting identity/account; null for automated events. Example: "67aa2b73dc3f7d0099887766"
    private String performedByDocsId;

    // Example: 2026-08-01T00:00:00Z
    @NotNull
    private Instant effectiveAt;
}
