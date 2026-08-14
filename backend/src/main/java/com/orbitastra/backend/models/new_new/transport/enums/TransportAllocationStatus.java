package com.orbitastra.backend.models.new_new.transport.enums;

/**
 * Whether a student is using the bus right now.
 *
 * <p>Named with the Transport prefix on purpose. Finance already has an
 * AllocationStatus, and that one is about splitting a payment across invoices.
 * The two mean completely different things and must not be mixed up.
 *
 * <p>Only an ACTIVE allocation is billed and only an ACTIVE allocation puts a
 * student on a trip list.
 */
public enum TransportAllocationStatus {
    /** Using the bus and being charged for it. */
    ACTIVE,

    /** Paused for a while, such as a long illness. Not billed, not on the list. */
    SUSPENDED,

    /** Finished, either at the end date or because the family stopped it. */
    ENDED
}
