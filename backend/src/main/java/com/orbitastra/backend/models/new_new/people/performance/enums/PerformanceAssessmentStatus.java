package com.orbitastra.backend.models.new_new.people.performance.enums;

/**
 * Lifecycle state of one staff performance assessment.
 */
public enum PerformanceAssessmentStatus {
    /** Respondent may still edit the assessment. */
    DRAFT,

    /** Respondent submitted the assessment. */
    SUBMITTED,

    /** Assessed staff member or manager acknowledged the assessment. */
    ACKNOWLEDGED,

    /** Authorized reviewer finalized the assessment. */
    FINALIZED,

    /** Assessment is no longer valid. */
    CANCELLED
}
