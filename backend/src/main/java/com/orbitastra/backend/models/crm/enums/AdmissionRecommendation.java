package com.orbitastra.backend.models.crm.enums;

/**
 * Decision recommended by an AdmissionReview. The final application decision
 * can combine multiple recommendations.
 */
public enum AdmissionRecommendation {
    /** Recommend approval. */
    APPROVE,

    /** Recommend rejection. */
    REJECT,

    /** Recommend placement on the waiting list. */
    WAITLIST,

    /** Review cannot finish until more information is supplied. */
    REQUEST_MORE_INFORMATION
}
