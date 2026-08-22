package com.orbitastra.backend.models.finance.enums;

/**
 * How a discount amount is worked out.
 *
 * <p>There is no separate "full waiver" type. A full waiver is PERCENT with the
 * share set to 100, so the sum is only done in one place.
 */
public enum ConcessionType {
    /** A share of the fee, such as 25 percent off. */
    PERCENT,

    /** A flat amount off one bill, such as 5000 off. */
    FIXED_AMOUNT
}
