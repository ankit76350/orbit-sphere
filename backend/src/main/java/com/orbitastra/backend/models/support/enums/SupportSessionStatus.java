package com.orbitastra.backend.models.support.enums;

/**
 * Whether one session of extra help actually happened.
 *
 * <p>NOT_DELIVERED is the state that makes this collection worth keeping. A plan promising two
 * remedial reading classes a week, where only six of forty happened, is a plan that failed —
 * and with only "scheduled" and "delivered" the missing thirty-four would sit as scheduled
 * forever and look like a diary rather than a failure.
 *
 * <p>STUDENT_ABSENT is deliberately separate from NOT_DELIVERED. A child who did not come is a
 * conversation with the family; a session the school did not run is a conversation with the
 * school. Counting them together lets the school blame the child.
 */
public enum SupportSessionStatus {
    /** Planned, not yet happened. */
    SCHEDULED,

    /** Happened. */
    DELIVERED,

    /** The child did not come. */
    STUDENT_ABSENT,

    /** The school did not run it, with the reason recorded. */
    NOT_DELIVERED,

    /** Called off in advance. */
    CANCELLED
}
