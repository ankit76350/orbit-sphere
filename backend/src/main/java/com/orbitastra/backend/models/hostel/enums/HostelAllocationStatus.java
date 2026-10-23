package com.orbitastra.backend.models.hostel.enums;

/**
 * Whether a child is living in the hostel right now.
 *
 * <p>Only ACTIVE is billed and only ACTIVE appears on a roll call. A child away for a
 * weekend is still ACTIVE: going home for two nights is a leave request, not a change
 * of residence, and treating it as one would take them off the roll call they most
 * need to be on.
 */
public enum HostelAllocationStatus {
    /** Living in the hostel and being charged for it. */
    ACTIVE,

    /** Paused for a long absence, such as a term away. Not billed, not on roll call. */
    SUSPENDED,

    /** Moved out, whether at the end of the year or early. */
    ENDED
}
