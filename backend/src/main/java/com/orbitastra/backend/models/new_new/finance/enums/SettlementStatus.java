package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How far a gateway payout has got in being matched to the bank and the books.
 *
 * <p>A gateway keeps its charges before paying the school, so the amount that
 * reaches the bank is smaller than the fees collected. Matching the batch is
 * what explains that difference.
 */
public enum SettlementStatus {
    /** The gateway has told us a payout is coming. */
    EXPECTED,

    /** The payout details have been loaded into the school's records. */
    IMPORTED,

    /** Some of the payments in the batch have been matched. */
    PARTIALLY_RECONCILED,

    /** Every payment in the batch has been matched to the bank. */
    RECONCILED,

    /** Raised with the gateway because the amount looks wrong. */
    DISPUTED
}
