package com.orbitastra.backend.models.new_new.people.recruitment.enums;

/**
 * Execution state of a recruitment interview.
 */
public enum InterviewStatus {
    /** Interview has been arranged. */
    SCHEDULED,

    /** Interview is currently taking place. */
    IN_PROGRESS,

    /** Interview and its evaluation are complete. */
    COMPLETED,

    /** Interview was cancelled. */
    CANCELLED
}
