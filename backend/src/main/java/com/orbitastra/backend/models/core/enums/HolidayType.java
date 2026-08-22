package com.orbitastra.backend.models.core.enums;

/**
 * Classification of a dated HolidayDetail entry.
 */
public enum HolidayType {
    /** A recurring weekly closure expanded into a concrete date. */
    WEEKLY_OFF,

    /** Government-declared public holiday. */
    PUBLIC_HOLIDAY,

    /** Cultural festival holiday. */
    FESTIVAL,

    /** Religion-related holiday. */
    RELIGIOUS,

    /** School event for which normal classes are closed or changed. */
    SCHOOL_EVENT,

    /** Date within a scheduled school vacation. */
    VACATION,

    /** Break associated with an examination schedule. */
    EXAM_BREAK,

    /** School-specific type not represented above. */
    OTHER
}
