package com.orbitastra.backend.models.common.enums;

/**
 * Guardian's relationship to a student.
 */
public enum GuardianRelation {
    /** Student's father. */
    FATHER,

    /** Student's mother. */
    MOTHER,

    /** Student's grandfather. */
    GRANDFATHER,

    /** Student's grandmother. */
    GRANDMOTHER,

    /** Student's uncle. */
    UNCLE,

    /** Student's aunt. */
    AUNT,

    /** Court- or law-recognized guardian. */
    LEGAL_GUARDIAN,

    /** Adult sibling acting as guardian/contact. */
    SIBLING,

    /** School-defined relationship not listed above. */
    OTHER
}
