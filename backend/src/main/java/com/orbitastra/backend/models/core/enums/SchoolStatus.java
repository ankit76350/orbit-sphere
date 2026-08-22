package com.orbitastra.backend.models.core.enums;

/**
 * Operational lifecycle of the School tenant itself.
 *
 * <p>This is separate from subscription status and from the RecordState used by
 * school-owned business documents.
 */
public enum SchoolStatus {
    /** School is using a trial onboarding path. */
    TRIAL,

    /** Tenant resources and configuration are being prepared. */
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
