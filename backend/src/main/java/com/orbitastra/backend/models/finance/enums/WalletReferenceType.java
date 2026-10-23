package com.orbitastra.backend.models.finance.enums;

/**
 * Which record caused a StoredValueLedgerEntry to be written.
 *
 * <p>Together with {@code referenceDocsId} this makes every wallet entry
 * traceable back to the payment, invoice or refund behind it, so a balance can
 * always be explained.
 */
public enum WalletReferenceType {
    /** A FeePayment that added money to or spent money from the wallet. */
    FEE_PAYMENT,

    /** A FeeInvoice that the wallet money was used for. */
    FEE_INVOICE,

    /** A RefundTransaction paid into or out of the wallet. */
    REFUND_TRANSACTION,

    /** Another wallet the money moved to or came from. */
    STORED_VALUE_ACCOUNT,

    /** A mess, canteen or other operational charge record. */
    OPERATIONAL_CHARGE,

    /** A correction typed in by a staff member with no source record. */
    MANUAL_ADJUSTMENT
}
