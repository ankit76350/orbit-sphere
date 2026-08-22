package com.orbitastra.backend.models.plans.enums;

/**
 * Contracted recurrence used to calculate subscription billing periods.
 */
public enum BillingCycle {
    /** One-month billing periods. */
    MONTHLY,

    /** Three-month billing periods. */
    QUARTERLY,

    /** Six-month billing periods. */
    HALF_YEARLY,

    /** Twelve-month billing periods. */
    YEARLY,

    /** Explicit dates supplied by the contracted subscription. */
    CUSTOM
}
