package com.orbitastra.backend.models.new_new.people.development.enums;

/**
 * Lifecycle state of a staff professional-development activity.
 */
public enum StaffDevelopmentStatus {
    /** Activity is proposed or scheduled. */
    PLANNED,

    /** Activity is waiting for authorization. */
    PENDING_APPROVAL,

    /** Activity is approved. */
    APPROVED,

    /** Staff member is currently participating. */
    IN_PROGRESS,

    /** Activity and required evidence are complete. */
    COMPLETED,

    /** Approval request was rejected. */
    REJECTED,

    /** Activity was cancelled. */
    CANCELLED
}
