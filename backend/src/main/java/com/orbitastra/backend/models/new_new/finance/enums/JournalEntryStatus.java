package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * State of one JournalEntry in the books.
 *
 * <p>Once an entry is POSTED its lines must never change. A mistake is fixed by
 * posting a reversing entry that points back at the wrong one.
 */
public enum JournalEntryStatus {
    /** Being prepared and not in the books yet. */
    DRAFT,

    /** Sent for approval and waiting for a decision. */
    PENDING_APPROVAL,

    /** In the books and now fixed. */
    POSTED,

    /** Cancelled by a matching reversing entry. */
    REVERSED,

    /** Written only to cancel an earlier entry. */
    REVERSAL,

    /** Turned down before it reached the books. */
    REJECTED
}
