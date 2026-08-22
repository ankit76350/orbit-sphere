package com.orbitastra.backend.models.new_new.student.enums;

/**
 * Overall lifecycle state of a Student profile.
 */
public enum StudentStatus {
    /** Student is currently enrolled with the school. */
    ACTIVE,

    /** Student profile is temporarily inactive. */
    INACTIVE,

    /** Student is temporarily suspended. */
    SUSPENDED,

    /** Student formally withdrew from the school. */
    WITHDRAWN,

    /** Student transferred to another school. */
    TRANSFERRED,

    /** Student completed the school's final grade. */
    GRADUATED,

    /** Former student retained for alumni services. */
    ALUMNI
}
