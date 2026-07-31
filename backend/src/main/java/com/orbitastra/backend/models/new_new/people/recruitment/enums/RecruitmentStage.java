package com.orbitastra.backend.models.new_new.people.recruitment.enums;

/**
 * Current recruitment stage of a candidate application.
 */
public enum RecruitmentStage {
    /** Candidate submitted the application. */
    APPLIED,

    /** Recruitment team is checking the application. */
    SCREENING,

    /** Candidate passed screening. */
    SHORTLISTED,

    /** One or more interviews are in progress. */
    INTERVIEW,

    /** Credentials, references, or background are being verified. */
    VERIFICATION,

    /** Employment offer was sent to the candidate. */
    OFFERED,

    /** Candidate accepted the employment offer. */
    ACCEPTED,

    /** School rejected the application. */
    REJECTED,

    /** Candidate withdrew the application. */
    WITHDRAWN,

    /** Candidate was converted to a Staff record. */
    HIRED
}
