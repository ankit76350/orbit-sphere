package com.orbitastra.backend.models.plans.enums;

/**
 * Immutable event classification stored in SubscriptionHistory.
 */
public enum SubscriptionEventType {
    /** Subscription document was created. */
    CREATED,

    /** Trial service period began. */
    TRIAL_STARTED,

    /** Paid or standard service became active. */
    ACTIVATED,

    /** Plan definition changed. */
    PLAN_CHANGED,
    /**
     * Contract terms changed without the status or the plan moving.
     *
     * <p>Price, billing cycle, period dates, capacity overrides, auto-renewal or the billing
     * customer reference. Added for #14, which can edit any of them: without it an edit either
     * went unrecorded or borrowed an event type that says something untrue, and "why is this
     * school's price different from its plan" would have no answer.
     */
    TERMS_CHANGED,

    /** A new subscription period began. */
    RENEWED,

    /** Billing moved the subscription into past-due state. */
    PAYMENT_PAST_DUE,

    /** Access was suspended. */
    SUSPENDED,

    /** Suspended access was restored. */
    RESUMED,

    /** Subscription was cancelled. */
    CANCELLED,

    /** Subscription reached its final period end. */
    EXPIRED
}
