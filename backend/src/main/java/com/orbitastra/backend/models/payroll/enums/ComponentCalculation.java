package com.orbitastra.backend.models.new_new.payroll.enums;

/**
 * How a component's amount is worked out.
 *
 * <p>Most components in an Indian school are a share of basic pay rather than a fixed
 * figure, which is why this exists. House rent allowance is normally 40 or 50 percent of
 * basic, and a school that raises basic pay expects it to follow automatically rather than
 * having to edit two numbers for every member of staff.
 */
public enum ComponentCalculation {
    /** A flat figure, the same every month until somebody changes it. */
    FIXED_AMOUNT,

    /** A share of basic pay. Example: house rent allowance at 40 percent of basic. */
    PERCENT_OF_BASIC,

    /** A share of total earnings. */
    PERCENT_OF_GROSS
}
