package com.orbitastra.backend.models.finance.enums;

/**
 * How often a fee head is charged to a student.
 *
 * <p>The frequency decides how many invoices a fee structure line produces in
 * one academic year. It does not decide the due dates; those come from the
 * installments listed on the fee structure.
 */
public enum FeeFrequency {
    /** Charged once for the whole academic year. */
    ONE_TIME,

    /** Charged every month. */
    MONTHLY,

    /** Charged every two months. */
    BI_MONTHLY,

    /** Charged every three months. */
    QUARTERLY,

    /** Charged twice in the year. */
    HALF_YEARLY,

    /** Charged once per year, but repeated in the next year. */
    ANNUAL,

    /** Charged only when someone raises it, such as a fine. */
    ON_DEMAND
}
