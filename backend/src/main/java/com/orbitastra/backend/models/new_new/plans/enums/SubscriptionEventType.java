package com.orbitastra.backend.models.new_new.plans.enums;

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
