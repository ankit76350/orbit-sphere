package com.orbitastra.backend.models.plans.billing.enums;

/**
 * Provider-processing state of one PaymentAttempt.
 */
public enum PaymentAttemptStatus {
    /** Attempt was created locally. */
    INITIATED,

    /** Provider is processing the attempt. */
    PROCESSING,

    /** Customer action such as OTP or 3-D Secure is required. */
    REQUIRES_ACTION,

    /** Provider confirmed successful collection. */
    SUCCEEDED,

    /** Provider or bank rejected the attempt. */
    FAILED,

    /** Attempt was cancelled before completion. */
    CANCELLED
}
