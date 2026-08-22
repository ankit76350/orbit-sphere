package com.orbitastra.backend.models.facilities.enums;

/**
 * How bad one thing found on an inspection is.
 *
 * <p>CRITICAL means the space should stop being used now, not that it is the worst thing on
 * this round. Keeping that meaning absolute rather than relative is what makes the field
 * usable: a query for critical findings should return the things that close a room, and if the
 * word drifts to mean "worst on the list" then every round produces one.
 */
public enum FindingSeverity {
    /** Worth writing down. Nothing needs to happen. */
    OBSERVATION,

    /** Should be fixed, no hurry. */
    MINOR,

    /** Needs fixing on a date somebody has agreed. */
    MAJOR,

    /** Stop using the space until this is dealt with. */
    CRITICAL
}
