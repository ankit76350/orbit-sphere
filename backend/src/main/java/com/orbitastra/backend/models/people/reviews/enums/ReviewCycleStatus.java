package com.orbitastra.backend.models.new_new.people.reviews.enums;

/**
 * Lifecycle state of a staff review cycle.
 */
public enum ReviewCycleStatus {
    /** Cycle configuration is still being prepared. */
    DRAFT,

    /** Cycle is configured to open later. */
    SCHEDULED,

    /** Reviewers may submit reviews. */
    OPEN,

    /** Submissions are closed and final review is underway. */
    IN_REVIEW,

    /** Cycle and results are finalized. */
    CLOSED,

    /** Cycle was cancelled. */
    CANCELLED
}
