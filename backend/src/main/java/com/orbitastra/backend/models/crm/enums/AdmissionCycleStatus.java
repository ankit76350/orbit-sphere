package com.orbitastra.backend.models.crm.enums;

/**
 * Lifecycle of an AdmissionCycle.
 */
public enum AdmissionCycleStatus {
    /** Configuration is incomplete and unavailable to applicants. */
    DRAFT,

    /** Configuration is complete but the opening date has not arrived. */
    SCHEDULED,

    /** Applications are currently accepted. */
    OPEN,

    /** New applications are no longer accepted. */
    CLOSED,

    /** All admission and enrollment processing for the cycle is finished. */
    COMPLETED,

    /** The cycle was cancelled and must not accept applications. */
    CANCELLED
}
