package com.orbitastra.backend.models.new_new.transport.enums;

/**
 * Which way a run of a route goes.
 *
 * <p>A route is normally run twice a day, once each way, and they are two separate
 * trips. A student may use only one of them, which is why the allocation records
 * the two directions separately.
 */
public enum TripDirection {
    /** Home to school in the morning. Students get on at their stop. */
    PICKUP,

    /** School to home in the afternoon. Students get off at their stop. */
    DROP
}
