package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Which finance record caused a JournalEntry to be posted.
 *
 * <p>Together with {@code sourceDocsId} and {@code idempotencyKey} this stops
 * the same event from being posted to the books twice, and lets an accountant
 * jump from a ledger line back to the invoice or payment behind it.
 */
public enum JournalSourceType {
    /** A fee invoice was issued. */
    FEE_INVOICE,

    /** A fee payment was received. */
    FEE_PAYMENT,

    /** A payment was spread across invoices. */
    PAYMENT_ALLOCATION,

    /** Money was sent back to a payer. */
    REFUND_TRANSACTION,

    /** A wallet balance changed. */
    WALLET_ENTRY,

    /** A gateway paid out a batch of collections to the bank. */
    SETTLEMENT_BATCH,

    /** A scholarship or concession covered part of a fee. */
    AID_AWARD,

    /** An unpaid amount was given up on. */
    WRITE_OFF,

    /** Typed in by an accountant with no source record. */
    MANUAL,

    /** The starting balances loaded when the books were set up. */
    OPENING_BALANCE,

    /** An entry made while closing a fiscal period. */
    PERIOD_CLOSE
}
