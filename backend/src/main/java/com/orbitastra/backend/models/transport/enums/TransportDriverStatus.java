package com.orbitastra.backend.models.transport.enums;

/** Whether a driver may be put on a route today. */
public enum TransportDriverStatus {
    /** Working and able to drive. */
    ACTIVE,

    /** Away, so somebody else has to cover the route. */
    ON_LEAVE,

    /** Stopped from driving while something is looked into. */
    SUSPENDED,

    /** No longer drives for the school. */
    RELIEVED
}
