package com.orbitastra.backend.models.facilities.enums;

/**
 * Where a request to use a space has got to.
 *
 * <p>Only APPROVED bookings hold the space. A REQUESTED booking does not block anybody else,
 * which is deliberate: if requests reserved the room, one person filling in a form for a
 * "maybe" in March would keep the hall empty until somebody remembered to cancel it.
 *
 * <p>That means two people can request the same slot, and approving the second has to fail.
 * The clash is caught at approval, which is the only moment the school actually commits.
 */
public enum BookingStatus {
    /** Asked for. Holds nothing. */
    REQUESTED,

    /** Granted. The space is held for that time. */
    APPROVED,

    /** Refused, with a reason. */
    REJECTED,

    /** Called off by whoever asked, or by the school. */
    CANCELLED,

    /** The time has passed and it was used. */
    COMPLETED
}
