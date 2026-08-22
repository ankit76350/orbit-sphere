package com.orbitastra.backend.models.plans.enums;

/**
 * Current commercial/service state of a SchoolSubscription.
 */
public enum SubscriptionStatus {
    /** Time-limited evaluation; currentPeriodEnd is the trial end boundary. */
    TRIAL,

    /** Subscription is valid and service is available. */
    ACTIVE,

    /** Payment is overdue but final suspension has not occurred. */
    PAST_DUE,

    /** Subscription access is blocked. */
    SUSPENDED,

    /** Subscription was cancelled and will not renew. */
    CANCELLED,

    /** Contracted period ended without renewal. */
    EXPIRED
}
