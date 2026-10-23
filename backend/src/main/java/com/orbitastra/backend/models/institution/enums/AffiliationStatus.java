package com.orbitastra.backend.models.institution.enums;

/**
 * Lifecycle of one AffiliationProgramme.
 */
public enum AffiliationStatus {
    /** Configuration is incomplete or not yet approved for use. */
    DRAFT,

    /** Affiliation is valid and available for academic configuration. */
    ACTIVE,

    /** Validity period ended. */
    EXPIRED,

    /** Board or school temporarily suspended the affiliation. */
    SUSPENDED,

    /** Affiliation was permanently revoked. */
    REVOKED
}
