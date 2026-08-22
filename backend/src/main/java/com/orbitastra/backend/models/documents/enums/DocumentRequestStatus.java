package com.orbitastra.backend.models.new_new.documents.enums;

/**
 * How far a request for a document has got.
 *
 * <p>ISSUED is the end of the line and is set only once the paper actually exists.
 * APPROVED means somebody said yes but the document has not been made yet, and the
 * two are kept apart so a request waiting on a printer is not mistaken for one
 * that is finished.
 */
public enum DocumentRequestStatus {
    /** Being filled in and not sent yet. */
    DRAFT,

    /** Sent in and waiting to be picked up. */
    SUBMITTED,

    /** Somebody is checking whether it can be given. */
    UNDER_REVIEW,

    /** Allowed, but the document has not been made yet. */
    APPROVED,

    /** Turned down, with the reason kept on the record. */
    REJECTED,

    /** The document has been made and handed over. */
    ISSUED,

    /** Taken back by the person who asked, before it was issued. */
    CANCELLED
}
