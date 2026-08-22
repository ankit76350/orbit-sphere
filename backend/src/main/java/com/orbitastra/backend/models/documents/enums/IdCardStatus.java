package com.orbitastra.backend.models.documents.enums;

/**
 * Whether an ID card may still be used.
 *
 * <p>A card that is not ACTIVE must not open a gate or mark a child onto a bus,
 * which is the whole reason the state is worth keeping. LOST matters most: a card
 * somebody else may be holding has to stop working the moment it is reported.
 */
public enum IdCardStatus {
    /** Working, and may be used to get in or to board. */
    ACTIVE,

    /** Passed its expiry date. */
    EXPIRED,

    /** Reported lost. Must stop working straight away. */
    LOST,

    /** Broken or unreadable, and being replaced. */
    DAMAGED,

    /** Taken back, usually because the person has left. */
    REVOKED
}
