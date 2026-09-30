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

    /**
     * Review cannot finish until more information is supplied.
     *
     * <p><b>And #27c enforces exactly that, from 2026-09-30.</b> A review carrying this cannot be
     * completed — {@code 409 RECOMMENDATION_NOT_FINAL} — because it is a reviewer asking for
     * something rather than a verdict, and a review closed on it would be a finished record of an
     * unfinished assessment. #20 would then count it among the reviews that are in.
     *
     * <p><b>It is not a dead end.</b> The reviewer asks, the family answers, and #27e replaces it
     * with APPROVE, REJECT or WAITLIST — the three that can be a review's last word.
     */
    REQUEST_MORE_INFORMATION
}
