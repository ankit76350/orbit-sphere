package com.orbitastra.backend.models.support.enums;

/**
 * How far the school has got in understanding a child's need.
 *
 * <p>SUSPECTED matters. A teacher noticing that a child cannot copy from the board is the start
 * of every one of these, and it is months before any specialist confirms anything. Without a
 * state for it, the concern lives in a teacher's head until they leave.
 *
 * <p>MONITORING is not the same as RESOLVED. A child who has caught up may fall behind again,
 * and a school that closes the record entirely loses the history the next teacher would want.
 */
public enum SupportNeedStatus {
    /** A teacher has noticed something. Nothing confirmed. */
    SUSPECTED,

    /** Sent for assessment and waiting on a report. */
    ASSESSMENT_REQUESTED,

    /** Confirmed, and a plan is in force. */
    ACTIVE,

    /** Doing well without active support, but still watched. */
    MONITORING,

    /** No longer needed. Kept for the record. */
    RESOLVED
}
