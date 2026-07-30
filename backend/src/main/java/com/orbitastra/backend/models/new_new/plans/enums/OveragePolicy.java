package com.orbitastra.backend.models.new_new.plans.enums;

/**
 * Behavior when a PlanFeature usage limit is reached.
 */
public enum OveragePolicy {
    /** Reject additional usage. */
    BLOCK,

    /** Allow usage while warning school administrators. */
    WARN,

    /** Allow additional usage without an automatic charge. */
    ALLOW,

    /** Allow usage and create billable overage records. */
    CHARGE
}
