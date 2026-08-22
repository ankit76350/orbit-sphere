package com.orbitastra.backend.models.new_new.compliance.enums;

/** What kind of thing the school has to do. */
public enum ComplianceRequirementType {
    /** Send data upward, such as the annual UDISE+ return. */
    DATA_SUBMISSION,

    /** Renew the board affiliation. */
    AFFILIATION_RENEWAL,

    /** Renew a licence or a no-objection certificate. */
    LICENCE_RENEWAL,

    /** Host an inspection or a visit. */
    INSPECTION,

    /** Have a safety check done, such as fire or electrical. */
    SAFETY_AUDIT,

    /** Have the books audited. */
    FINANCIAL_AUDIT,

    /** Put staff through required training. */
    MANDATORY_TRAINING,

    /** Get every student registered, such as for APAAR. */
    STUDENT_REGISTRATION,

    /** File a statutory return, such as provident fund or income tax. */
    STATUTORY_FILING,

    /** Anything the types above do not cover. */
    OTHER
}
