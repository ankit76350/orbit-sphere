package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Whether a StoredValueAccount can be used right now.
 */
public enum WalletAccountStatus {
    /** Money can be paid in and taken out. */
    ACTIVE,

    /** Temporarily blocked, so no money can move either way. */
    FROZEN,

    /** Closed after the balance was paid out or moved. */
    CLOSED
}
