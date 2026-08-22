package com.orbitastra.backend.models.new_new.hostel.enums;

/**
 * Who a hostel building takes.
 *
 * <p>Kept on the building rather than worked out from who is in it, because it is a
 * rule the school sets before anybody moves in, and the service uses it to refuse an
 * allocation that would put a child in the wrong building.
 */
public enum HostelType {
    /** Boys only. */
    BOYS,

    /** Girls only. */
    GIRLS,

    /** Staff or visiting faculty rather than students. */
    STAFF
}
