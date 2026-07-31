package com.orbitastra.backend.models.new_new.people.recruitment.enums;

/**
 * Lifecycle state of an approved staff hiring requirement.
 */
public enum JobVacancyStatus {
    /** Vacancy is still being prepared. */
    DRAFT,

    /** Vacancy is waiting for authorized approval. */
    PENDING_APPROVAL,

    /** Vacancy is approved but not yet accepting applications. */
    APPROVED,

    /** Vacancy is accepting applications. */
    OPEN,

    /** Vacancy no longer accepts applications. */
    CLOSED,

    /** All required positions have been filled. */
    FILLED,

    /** Hiring requirement was cancelled. */
    CANCELLED
}
