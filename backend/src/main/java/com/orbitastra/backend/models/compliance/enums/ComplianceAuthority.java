package com.orbitastra.backend.models.compliance.enums;

/** Who the school has to answer to. */
public enum ComplianceAuthority {
    /** Central Board of Secondary Education. */
    CBSE,

    /** Council for the Indian School Certificate Examinations. */
    CISCE,

    /** The state's own school education board. */
    STATE_BOARD,

    /** Unified District Information System for Education. */
    UDISE,

    /** The APAAR and academic bank of credits registry. */
    APAAR_REGISTRY,

    /** The state or district education department. */
    EDUCATION_DEPARTMENT,

    /** The local municipal or panchayat body. */
    MUNICIPAL_BODY,

    /** The fire service, for a no-objection certificate. */
    FIRE_DEPARTMENT,

    /** The health department, for kitchen and water clearances. */
    HEALTH_DEPARTMENT,

    /** The transport authority, for bus fitness and permits. */
    TRANSPORT_AUTHORITY,

    /** The income tax department. */
    INCOME_TAX,

    /** Provident fund or state insurance authorities. */
    LABOUR_AUTHORITY,

    /** Anybody the list above does not cover. */
    OTHER
}
