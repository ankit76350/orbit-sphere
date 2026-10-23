package com.orbitastra.backend.models.plans.billing.enums;

/**
 * Lifecycle of a SubscriptionPayment aggregate.
 */
public enum SubscriptionPaymentStatus {
    /** Payment exists but has not completed. */
    PENDING,

    /** Full payment amount was successfully received. */
    SUCCEEDED,

    /** Payment failed; detailed attempts retain provider errors. */
    FAILED,

    /** Payment was cancelled before completion. */
    CANCELLED,

    /** Part of a successful payment was returned. */
    PARTIALLY_REFUNDED,

    /** Entire successful payment was returned. */
    REFUNDED
}
