package com.orbitastra.backend.models.new_new.hostel.enums;

/**
 * How far a request to go home has got.
 *
 * <p>DEPARTED and OVERDUE are the two that matter at night. A child at DEPARTED is not
 * in the building and is not missing. A child at OVERDUE should have been back by now
 * and nobody has seen them, which is the state a warden has to act on.
 */
public enum HostelLeaveStatus {
    /** Being filled in, not sent yet. */
    DRAFT,

    /** Sent in and waiting for the warden. */
    PENDING_APPROVAL,

    /** Allowed, but the child has not gone yet. */
    APPROVED,

    /** Refused, with the reason kept on the record. */
    REJECTED,

    /** The child has left the hostel. */
    DEPARTED,

    /** The child is back. */
    RETURNED,

    /** Should have been back and is not. Somebody has to ring home. */
    OVERDUE,

    /** Called off before the child went. */
    CANCELLED
}
