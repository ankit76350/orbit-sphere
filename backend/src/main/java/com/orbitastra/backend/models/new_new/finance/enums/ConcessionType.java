package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How a concession amount is worked out.
 *
 * <p>A full waiver is not a type of its own. It is PERCENT with the share set to
 * 100, which keeps the sum in one place instead of two.
 */
public enum ConcessionType {
    /** A share of the eligible fee, such as 25 percent off. */
    PERCENT,

    /** A flat money amount off one bill, such as 5000 off. */
    FIXED_AMOUNT
}
