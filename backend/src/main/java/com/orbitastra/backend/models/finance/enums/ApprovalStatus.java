package com.orbitastra.backend.models.finance.enums;

/**
 * Shared maker-checker state for finance records that one user raises and a
 * different user decides, such as a ConcessionRequest.
 *
 * <p>The rule that the person who raised the record cannot also approve it is
 * checked by the service, not by this enum.
 */
public enum ApprovalStatus {
    /** Being prepared by the person raising it. */
    DRAFT,

    /** Submitted and waiting for a decision. */
    PENDING_APPROVAL,

    /** Accepted and now in effect. */
    APPROVED,

    /** Turned down, with the reason stored on the record. */
    REJECTED,

    /** Taken back by the person who raised it before any decision. */
    WITHDRAWN
}
