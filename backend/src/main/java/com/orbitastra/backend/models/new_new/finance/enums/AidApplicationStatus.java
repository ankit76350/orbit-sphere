package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How far a family's application for financial help has got.
 *
 * <p>Checking the paperwork and deciding the outcome are separate steps, so the
 * person who verifies the documents is not the person who approves the money.
 */
public enum AidApplicationStatus {
    /** Started by the family but not sent in yet. */
    DRAFT,

    /** Sent in and waiting to be picked up. */
    SUBMITTED,

    /** Someone is checking the income proof and other documents. */
    UNDER_VERIFICATION,

    /** Documents are checked and a committee is deciding. */
    UNDER_REVIEW,

    /** Accepted, and an AidAward should now exist for it. */
    APPROVED,

    /** Turned down, with the reason stored on the record. */
    REJECTED,

    /** Taken back by the family before a decision. */
    WITHDRAWN,

    /** Closed because the family did not send the documents in time. */
    LAPSED
}
