package com.orbitastra.backend.models.undone.compliance.enums;

/**
 * The purpose for which consent is being requested.
 * Example: "Photos & Media", "Health Data", "Transport"
 */
public enum ConsentPurpose {
    /**
     * Consent for using photographs or videos.
     */
    PHOTOGRAPHS,
    /**
     * Consent for using health data.
     */
    HEALTH_DATA,
    /**
     * Consent for using transport-related data.
     */
    TRANSPORT,
    /**
     * Consent for using academic or performance data.
     */
    ACADEMIC_PERFORMANCE,
    /**
     * Consent for using contact or communication data.
     */
    CONTACT_COMMUNICATION,
    /**
     * Consent for using sensitive personal data.
     */
    SENSITIVE_DATA,
    /**
     * Consent for other purposes.
     */
    OTHER
}
