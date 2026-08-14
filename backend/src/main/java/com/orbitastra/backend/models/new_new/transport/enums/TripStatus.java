package com.orbitastra.backend.models.new_new.transport.enums;

/** How far one run of a route has got. */
public enum TripStatus {
    /** Planned for today but not started. */
    SCHEDULED,

    /** On the road now. */
    IN_PROGRESS,

    /** Finished normally. */
    COMPLETED,

    /** Did not run, with the reason kept on the trip. */
    CANCELLED
}
