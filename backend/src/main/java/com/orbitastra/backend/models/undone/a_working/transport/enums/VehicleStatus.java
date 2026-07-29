package com.orbitastra.backend.models.undone.a_working.transport.enums;



public enum VehicleStatus {

    /**
     * Ready for operation.
     */
    ACTIVE,

    /**
     * Currently assigned to a trip.
     */
    IN_SERVICE,

    /**
     * Under maintenance.
     */
    UNDER_MAINTENANCE,

    /**
     * Temporarily unavailable.
     */
    OUT_OF_SERVICE,

    /**
     * Permanently retired.
     */
    RETIRED

}
