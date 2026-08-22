package com.orbitastra.backend.models.new_new.transport.enums;

/**
 * Whether a vehicle can be put on a route today.
 *
 * <p>Only an ACTIVE vehicle may be assigned to a route or start a trip. The other
 * three all mean "not today", and the reason is kept in
 * {@code TransportVehicle.statusReason}.
 */
public enum VehicleStatus {
    /** Working and able to run. */
    ACTIVE,

    /** In the workshop. */
    UNDER_MAINTENANCE,

    /** Off the road, usually because a paper such as fitness has run out. */
    OUT_OF_SERVICE,

    /** Sold or scrapped. Kept so old trips still make sense. */
    RETIRED
}
