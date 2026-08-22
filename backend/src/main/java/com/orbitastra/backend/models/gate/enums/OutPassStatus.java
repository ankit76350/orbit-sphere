package com.orbitastra.backend.models.new_new.gate.enums;

/**
 * How far a request to take a child out early has got.
 *
 * <p>EXITED is the state to watch. A child at EXITED is out of the school during
 * school hours, and if they were expected back and the state has not moved to
 * RETURNED, somebody has to find out why.
 */
public enum OutPassStatus {
    /** Being filled in, not sent yet. */
    DRAFT,

    /** Sent in and waiting for somebody to allow it. */
    PENDING_APPROVAL,

    /** Allowed, but the child has not left yet. */
    APPROVED,

    /** Refused, with the reason kept on the record. */
    REJECTED,

    /** The child has left the school. */
    EXITED,

    /** The child has come back. */
    RETURNED,

    /** Allowed but never used, and the day has passed. */
    EXPIRED,

    /** Taken back by the family or the school before the child left. */
    CANCELLED
}
