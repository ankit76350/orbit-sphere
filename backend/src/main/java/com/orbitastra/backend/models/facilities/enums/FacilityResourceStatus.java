package com.orbitastra.backend.models.new_new.facilities.enums;

/**
 * Whether a space can be used.
 *
 * <p>UNDER_MAINTENANCE and CLOSED are separate because they mean different things to whoever
 * is looking for a room. Under maintenance is temporary and somebody is working on it; closed
 * is a decision that it is not to be used, which may have no work planned at all. A lab closed
 * because the school has no chemistry teacher this year is not waiting for a plumber.
 *
 * <p>DECOMMISSIONED is the end. The row stays, because inspections and work orders point at it
 * and a demolished building still has a history somebody may be asked about.
 */
public enum FacilityResourceStatus {
    /** In normal use. */
    IN_USE,

    /** Temporarily out of use while work happens. */
    UNDER_MAINTENANCE,

    /** Not to be used, with no work necessarily planned. */
    CLOSED,

    /** Gone: demolished, sold, or no longer part of the school. History kept. */
    DECOMMISSIONED
}
