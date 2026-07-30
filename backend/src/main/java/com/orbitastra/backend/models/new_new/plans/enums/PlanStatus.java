package com.orbitastra.backend.models.new_new.plans.enums;

/**
 * Publication lifecycle of one PlanDefinition version.
 */
public enum PlanStatus {
    /** Editable and unavailable for new subscriptions. */
    DRAFT,

    /** Available within its effective dates. */
    ACTIVE,

    /** Unavailable for new subscriptions but retained for existing history. */
    RETIRED
}
