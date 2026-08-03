package com.orbitastra.backend.models.new_new.people.performance.enums;

/**
 * Lifecycle state of a staff performance cycle.
 */
public enum PerformanceCycleStatus {
    /** Cycle configuration is still being prepared. */
    DRAFT,

    /** Cycle is configured to open later. */
    SCHEDULED,

    /** Reviewers may submit assessments. */
    OPEN,

    /** Submissions are closed and final review is underway. */
    IN_REVIEW,

    /** Cycle and results are finalized. */
    CLOSED,

    /** Cycle was cancelled. */
    CANCELLED
}
