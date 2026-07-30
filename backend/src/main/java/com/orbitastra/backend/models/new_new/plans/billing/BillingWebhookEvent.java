package com.orbitastra.backend.models.new_new.plans.billing;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.plans.billing.enums.WebhookProcessingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "billing_webhook_events")
@CompoundIndexes({
        @CompoundIndex(
                name = "billing_provider_event_uniq",
                def = "{'gatewayProvider': 1, 'providerEventId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_webhook_processing_idx",
                def = "{'schoolId': 1, 'processingStatus': 1, 'nextRetryAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWebhookEvent extends SchoolBase {

    // Example: "RAZORPAY"
    private String gatewayProvider;

    // Example: "evt_R7pw2V6n8"
    private String providerEventId;

    // Example: "payment.captured"
    private String providerEventType;

    // Example: WebhookProcessingStatus.RECEIVED
    private WebhookProcessingStatus processingStatus;

    // Example: true
    private Boolean signatureValid;

    // Example: "sha256:9af3d98e..."
    private String payloadHash;

    // Example: "kms:v1:encrypted-webhook-payload"
    private String encryptedPayload;

    // Example: "SUBSCRIPTION_PAYMENT"
    private String relatedEntityType;

    // Example: "67ab19f4dc3f7d0077889900"
    private String relatedEntityDocsId;

    // Example: 1
    private Integer processingAttemptCount;

    // Example: 2026-04-10T09:15:01Z
    private Instant receivedAt;

    // Example: 2026-04-10T09:15:03Z
    private Instant processedAt;

    // Example: 2026-04-10T09:20:00Z
    private Instant nextRetryAt;

    // Example: "INVOICE_NOT_FOUND"
    private String failureCode;

    // Example: "The referenced subscription invoice could not be resolved."
    private String failureMessage;
}
