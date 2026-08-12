package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * State of one RefundTransaction, from the request to the money going back.
 */
public enum RefundStatus {
    /** Raised and waiting to be sent for approval. */
    REQUESTED,

    /** Sent for approval and waiting for a decision. */
    PENDING_APPROVAL,

    /** Approved but the money has not gone out yet. */
    APPROVED,

    /** Turned down, with the reason stored on the record. */
    REJECTED,

    /** Being sent back through the gateway or the bank. */
    PROCESSING,

    /** The payer has received the money. */
    COMPLETED,

    /** The attempt to send the money back did not go through. */
    FAILED,

    /** Called off before the money went out. */
    CANCELLED
}
