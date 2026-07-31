package com.orbitastra.backend.models.new_new.people.recruitment.enums;

/**
 * Interview panel's hiring recommendation.
 */
public enum InterviewRecommendation {
    /** Candidate is exceptionally suitable. */
    STRONG_SELECT,

    /** Candidate is suitable for selection. */
    SELECT,

    /** Decision should be postponed for comparison or more evidence. */
    HOLD,

    /** Candidate should not proceed. */
    REJECT
}
