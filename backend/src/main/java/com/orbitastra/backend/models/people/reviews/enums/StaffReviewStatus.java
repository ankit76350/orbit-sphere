package com.orbitastra.backend.models.people.reviews.enums;

/**
 * Lifecycle state of one staff review.
 */
public enum StaffReviewStatus {
    /** Reviewer may still edit the review. */
    DRAFT,

    /** Reviewer submitted the review. */
    SUBMITTED,

    /** Reviewed staff member or manager acknowledged the review. */
    ACKNOWLEDGED,

    /** Authorized reviewer finalized the review. */
    FINALIZED,

    /** Review is no longer valid. */
    CANCELLED
}
