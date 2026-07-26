package com.orbitastra.backend.models.undone.compliance.enums;


/**
 * Type of regulatory or compliance task that a school must complete.
 */
public enum ComplianceTaskType {

    /**
     * Submission of mandatory data or reports.
     */
    DATA_SUBMISSION,

    /**
     * Renewal of school affiliation or recognition.
     */
    AFFILIATION_RENEWAL,

    /**
     * Renewal of licenses or certificates.
     */
    LICENSE_RENEWAL,

    /**
     * Government or board inspection.
     */
    INSPECTION,

    /**
     * Safety-related audit (fire, structural, electrical, etc.).
     */
    SAFETY_AUDIT,

    /**
     * Financial or statutory audit.
     */
    AUDIT,

    /**
     * Staff or student compliance training.
     */
    TRAINING,

    /**
     * Policy or legal document submission.
     */
    DOCUMENT_SUBMISSION,

    /**
     * Renewal of mandatory certificates.
     */
    CERTIFICATE_RENEWAL,

    /**
     * Any other compliance-related task.
     */
    OTHER
}