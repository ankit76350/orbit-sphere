package com.orbitastra.backend.models.institution.enums;

/**
 * Boundary at which a NumberSequence may start again from its configured first
 * value. Reset execution is performed atomically by the sequence service.
 */
public enum SequenceResetPolicy {
    /** Counter never resets. */
    NEVER,

    /** Counter resets at the start of a calendar year. */
    CALENDAR_YEAR,

    /** Counter resets when a new academic-year scope is used. */
    ACADEMIC_YEAR,

    /** Counter resets for each calendar month. */
    MONTHLY
}
