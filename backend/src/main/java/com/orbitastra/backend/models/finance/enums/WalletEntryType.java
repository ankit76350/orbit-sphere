package com.orbitastra.backend.models.finance.enums;

/**
 * Why one StoredValueLedgerEntry was written.
 *
 * <p>Each type belongs to a fixed direction, which the service checks. For
 * example a TOP_UP is always a credit and a FEE_PAYMENT is always a debit.
 */
public enum WalletEntryType {
    /** A parent added money to the wallet. */
    TOP_UP,

    /** Wallet money was used to pay a fee invoice. */
    FEE_PAYMENT,

    /** A charge such as a mess or canteen bill was taken from the wallet. */
    CHARGE,

    /** A refund from the school was put into the wallet. */
    REFUND_IN,

    /** Wallet money was paid back out to the parent. */
    REFUND_OUT,

    /** Money was moved to or from another wallet of the same owner. */
    TRANSFER,

    /** A staff member corrected the balance by hand, with a reason. */
    ADJUSTMENT,

    /** Money was set aside and cannot be spent yet. */
    HOLD,

    /** Money that was set aside is available to spend again. */
    HOLD_RELEASE,

    /** The remaining balance was paid out while closing the wallet. */
    CLOSURE_PAYOUT,

    /** Written only to cancel an earlier entry. */
    REVERSAL
}
