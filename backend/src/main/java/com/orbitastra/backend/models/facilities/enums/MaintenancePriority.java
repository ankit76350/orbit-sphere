package com.orbitastra.backend.models.new_new.facilities.enums;

/**
 * How soon a maintenance job needs doing.
 *
 * <p>EMERGENCY has one meaning and it is narrow: **somebody could be hurt before tomorrow.** A
 * live wire hanging in a corridor, a stair railing that has come away, a gas leak in the
 * kitchen. It is not "the principal is annoyed about it".
 *
 * <p>Keeping that meaning narrow is the whole value of the field. A priority scale where
 * EMERGENCY means "important" fills up with important things, and then the one job that was
 * actually dangerous is the fourth item on a list of eleven emergencies.
 *
 * <p>Unlike the reporting channel in `feedback`, a scale is right here rather than a single
 * question, because the person raising a maintenance job is usually staff who can judge it, and
 * the middle of the scale does real work in ordering a week's jobs.
 */
public enum MaintenancePriority {
    /** Would be nice. Whenever there is time. */
    LOW,

    /** The ordinary case. Fits into the normal run of work. */
    NORMAL,

    /** Getting in the way of teaching or living. Should jump the queue. */
    HIGH,

    /** Somebody could be hurt before tomorrow. */
    EMERGENCY
}
