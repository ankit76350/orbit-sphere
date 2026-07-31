package com.orbitastra.backend.models.new_new.people.performance.enums;

/**
 * Relationship of a performance-assessment respondent to the staff member.
 */
public enum PerformanceRespondentType {
    /** Staff member assesses themself. */
    SELF,

    /** Staff member's manager assesses them. */
    MANAGER,

    /** Another staff member assesses them. */
    PEER,

    /** Student provides feedback where the school permits it. */
    STUDENT,

    /** Parent or guardian provides feedback where permitted. */
    PARENT,

    /** School-defined respondent category. */
    OTHER
}
