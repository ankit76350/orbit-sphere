package com.orbitastra.backend.models.student.enums;

/**
 * State of one academic-year StudentAcademicRecord.
 */
public enum AcademicRecordStatus {

    /** This is the student's current placement for the academic year. */
    ACTIVE,

    /** Student completed this placement normally. */
    COMPLETED,

    /** Placement ended because the student changed class or section. */
    TRANSFERRED,

    /** Academic record was created but later cancelled. */
    CANCELLED
}
