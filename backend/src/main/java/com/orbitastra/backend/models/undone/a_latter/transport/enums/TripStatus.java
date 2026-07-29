package com.orbitastra.backend.models.undone.a_latter.transport.enums;


public enum TripStatus {

    /**
     * Driver has not started the trip.
     */
    NOT_STARTED,

    /**
     * Vehicle has started and is moving.
     */
    ON_ROUTE,

    /**
     * Vehicle is currently stopped
     * at a pickup/drop stop.
     */
    AT_STOP,

    /**
     * Trip has finished.
     */
    COMPLETED,

    /**
     * Emergency situation.
     */
    EMERGENCY

}