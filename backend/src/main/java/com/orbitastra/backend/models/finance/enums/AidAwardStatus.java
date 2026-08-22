package com.orbitastra.backend.models.finance.enums;

/**
 * Whether an approved AidAward may still reduce a student's fees.
 */
public enum AidAwardStatus {
    /** In force, so new invoices may use it. */
    ACTIVE,

    /** Paused, usually because a condition such as attendance was not met. */
    SUSPENDED,

    /** Taken back before it ran out. */
    WITHDRAWN,

    /** Ran to its end date or used up its full amount. */
    COMPLETED,

    /** Still active but needs to be renewed for the next year. */
    RENEWAL_DUE,

    /** Ended because it was not renewed in time. */
    EXPIRED
}
