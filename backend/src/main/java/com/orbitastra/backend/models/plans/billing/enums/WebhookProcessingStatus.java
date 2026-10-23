package com.orbitastra.backend.models.plans.billing.enums;

/**
 * Internal processing lifecycle of a BillingWebhookEvent.
 */
public enum WebhookProcessingStatus {
    /** Event was durably received but not yet verified. */
    RECEIVED,

    /** Provider signature and basic authenticity checks passed. */
    VERIFIED,

    /** Event was successfully and idempotently applied. */
    PROCESSED,

    /** Processing failed and may be retried. */
    FAILED,

    /** Valid event required no business action. */
    IGNORED,

    /** Retry limit was exhausted and manual intervention is required. */
    DEAD_LETTER
}
