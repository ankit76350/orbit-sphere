package com.orbitastra.backend.models.new_new.compliance.enums;

/**
 * How often something comes round again.
 *
 * <p>This is what lets the school be warned before a deadline instead of after it. A
 * requirement that repeats can have the next submission created automatically once the last
 * one is filed, so nobody has to remember in eleven months' time.
 */
public enum RequirementFrequency {
    /** Happens once and does not come back. */
    ONE_TIME,

    /** Every month. */
    MONTHLY,

    /** Every three months. */
    QUARTERLY,

    /** Twice a year. */
    HALF_YEARLY,

    /** Once a year. */
    ANNUAL,

    /** Every few years, with the gap stated on the requirement. */
    MULTI_YEAR
}
