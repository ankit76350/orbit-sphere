package com.orbitastra.backend.models.hostel.enums;

/**
 * Whether one bed can be given to a child.
 *
 * <p>Only AVAILABLE may be allocated. The status lives on the bed so a warden looking
 * for space sees it in one read, without going through every allocation in the
 * building to work out which beds are free.
 *
 * <p>Who is actually in an OCCUPIED bed is **not** stored on the bed. That is the
 * allocation's job, and keeping it in one place is what stops the bed and the
 * allocation disagreeing about who sleeps where.
 */
public enum BedStatus {
    /** Empty and ready. */
    AVAILABLE,

    /** Somebody has been allocated to it. */
    OCCUPIED,

    /** Held for a child who is arriving, so nobody else is put there. */
    RESERVED,

    /** Broken, or the room is being repaired. */
    UNDER_MAINTENANCE,

    /** Taken out of use for good. */
    WITHDRAWN
}
