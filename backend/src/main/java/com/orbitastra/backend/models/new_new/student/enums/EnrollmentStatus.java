package com.orbitastra.backend.models.new_new.student.enums;

/**
 * State of one academic-year StudentEnrollment record.
 */
public enum EnrollmentStatus {
    /** Enrollment is prepared but not yet active. */
    PLANNED,

    /** This is the student's current placement for the academic year. */
    ACTIVE,

    /** Student completed this placement normally. */
    COMPLETED,

    /** Placement ended because the student changed class or section. */
    TRANSFERRED,

    /** Placement ended because the student withdrew. */
    WITHDRAWN,

    /** Enrollment was created but later cancelled. */
    CANCELLED
}
