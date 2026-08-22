package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * State of a UPI AutoPay mandate that lets the school debit fees automatically.
 */
public enum MandateStatus {
    /** Sent to the parent's UPI app and waiting for them to allow it. */
    PENDING_AUTHORIZATION,

    /** Allowed by the parent, so debits may be raised. */
    ACTIVE,

    /** Temporarily stopped by the school or the parent. */
    PAUSED,

    /** Cancelled and cannot be used again. */
    CANCELLED,

    /** Passed its end date. */
    EXPIRED,

    /** The parent's bank or app refused to set it up. */
    FAILED
}
