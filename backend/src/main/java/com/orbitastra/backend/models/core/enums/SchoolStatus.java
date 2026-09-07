package com.orbitastra.backend.models.core.enums;

/**
 * Operational lifecycle of the School tenant itself.
 *
 * <p>This is separate from subscription status and from the RecordState used by
 * school-owned business documents.
 */
public enum SchoolStatus {
    /**
     * Tenant resources and configuration are being prepared. Where every school starts.
     *
     * <p>There is no separate trial state. A trial is a property of what a school is PAYING for,
     * not of the school itself — it lives on the subscription as {@code SubscriptionStatus.TRIAL},
     * which has a plan and a period behind it. A school-level TRIAL existed here once and never
     * behaved differently from PROVISIONING anywhere in the codebase: the same three checks
     * accepted both, so it was a second word for one state.
     */
    PROVISIONING,

    /** Tenant is available for normal use. */
    ACTIVE,

    /** Tenant access is temporarily blocked. */
    SUSPENDED,

    /** Data export, contract closure, and shutdown work is in progress. */
    OFFBOARDING,

    /** Tenant is closed but retained under applicable retention rules. */
    CLOSED,

    /** Permanent deletion has been requested but not yet executed. */
    DELETION_PENDING,

    /** Tenant has completed logical deletion and awaits or has completed purge. */
    DELETED
}
