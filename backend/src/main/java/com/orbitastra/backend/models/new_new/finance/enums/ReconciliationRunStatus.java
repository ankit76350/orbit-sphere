package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * State of one attempt to match a bank statement against the books.
 */
public enum ReconciliationRunStatus {
    /** Statement lines are being matched. */
    IN_PROGRESS,

    /** Matching is done but someone still has to sign it off. */
    PENDING_REVIEW,

    /** Signed off, with every line either matched or explained. */
    COMPLETED,

    /** Given up on, usually because the statement was wrong. */
    ABANDONED
}
