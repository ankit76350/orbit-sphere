package com.orbitastra.backend.models.plans.billing;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.plans.billing.enums.WebhookProcessingStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Durable, idempotent record of one payment-provider webhook.
 *
 * <p>The provider event id is unique per gateway. The payload is encrypted at
 * rest and represented by a verification hash; secrets and raw signing keys are
 * never stored. The integration must resolve and verify the School tenant before
 * persisting this SchoolBase document.
 *
 * <p>Webhook processing updates this record and uses relatedEntityType plus
 * relatedEntityDocsId to link to the affected invoice, payment, attempt, or
 * subscription after resolution.
 */
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
    @NotBlank
    private String gatewayProvider;

    // Example: "evt_R7pw2V6n8"
    @NotBlank
    private String providerEventId;

    // Example: "payment.captured"
    @NotBlank
    private String providerEventType;

    // Example: WebhookProcessingStatus.RECEIVED
    @NotNull
    @Builder.Default
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.RECEIVED;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean signatureValid = false;

    // Example: "sha256:9af3d98e..."
    @NotBlank
    private String payloadHash;

    // Example: "kms:v1:encrypted-webhook-payload"
    @NotBlank
    private String encryptedPayload;

    // Example: "SUBSCRIPTION_PAYMENT"
    private String relatedEntityType;

    // Example: "67ab19f4dc3f7d0077889900"
    private String relatedEntityDocsId;

    // Example: 1
    @NotNull
    @Builder.Default
    private Integer processingAttemptCount = 0;

    // Example: 2026-04-10T09:15:01Z
    @NotNull
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
