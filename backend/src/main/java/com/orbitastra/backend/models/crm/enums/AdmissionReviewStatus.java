package com.orbitastra.backend.models.crm.enums;

/**
 * Work status of one AdmissionReview assignment.
 */
public enum AdmissionReviewStatus {
    /** Assigned but not started. */
    PENDING,

    /** Reviewer is currently evaluating the application. */
    IN_PROGRESS,

    /** Reviewer submitted the final result. */
    COMPLETED,

    /** Review assignment was cancelled and has no decision. */
    CANCELLED
}
