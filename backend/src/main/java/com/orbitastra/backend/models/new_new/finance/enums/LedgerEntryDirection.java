package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Which way money moved in one StoredValueLedgerEntry.
 *
 * <p>The direction is kept separate from the reason for the entry so a report
 * can add up money in and money out without listing every entry type.
 */
public enum LedgerEntryDirection {
    /** Money went into the account and the balance went up. */
    CREDIT,

    /** Money went out of the account and the balance went down. */
    DEBIT
}
