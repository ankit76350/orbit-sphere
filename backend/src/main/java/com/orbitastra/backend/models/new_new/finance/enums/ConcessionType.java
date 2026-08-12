package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How a concession amount is worked out.
 */
public enum ConcessionType {
    /** A share of the fee, such as 25 percent off. */
    PERCENT,

    /** A flat money amount off, such as 5000 off. */
    FIXED_AMOUNT,

    /** The whole fee is waived. */
    FULL_WAIVER
}
