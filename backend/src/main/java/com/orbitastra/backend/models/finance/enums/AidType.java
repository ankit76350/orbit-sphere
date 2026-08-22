package com.orbitastra.backend.models.finance.enums;

/**
 * What kind of help an AidProgramme gives.
 */
public enum AidType {
    /** Given for good marks or results. */
    MERIT,

    /** Given because the family cannot afford the full fee. */
    NEED_BASED,

    /** Free or reduced seats required under the Right to Education rules. */
    RTE_EWS,

    /** Discount when more than one child from a family studies here. */
    SIBLING,

    /** Discount for the child of a staff member. */
    STAFF_CHILD,

    /** Given for sports achievement. */
    SPORTS,

    /** Paid by an outside sponsor for a named student. */
    SPONSOR,

    /** Paid from donated funds. */
    DONOR,

    /** Paid under a government scholarship scheme. */
    GOVERNMENT_SCHEME,

    /** Anything the types above do not cover. */
    OTHER
}
