package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How far one bank statement line has got in being matched to the books.
 */
public enum ReconciliationMatchState {
    /** Nothing in the books has been found for this line yet. */
    UNMATCHED,

    /** A likely match was found automatically and needs a person to confirm it. */
    SUGGESTED,

    /** Confirmed against a payment or a journal entry. */
    MATCHED,

    /** Deliberately left out, such as a bank charge handled elsewhere. */
    IGNORED,

    /** Raised with the bank or the gateway because it looks wrong. */
    DISPUTED
}
