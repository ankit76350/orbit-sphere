package com.orbitastra.backend.models.finance.enums;

/**
 * What caused a FeeInvoice to be created.
 *
 * <p>Together with {@code sourceDocsId} this lets a charge from any part of the
 * school be billed through the one invoice collection, so hostel, transport and
 * library charges do not each need their own billing models.
 */
public enum ChargeSourceType {
    /** Created by applying a FeeStructure to a student. */
    FEE_STRUCTURE,

    /** Raised for a bus or van seat. */
    TRANSPORT,

    /** Raised for a hostel stay. */
    HOSTEL,

    /** Raised for mess or canteen service. */
    MESS,

    /** Raised as a library penalty, such as a late or lost book. */
    LIBRARY_FINE,

    /** Raised for an exam or a re-exam. */
    EXAMINATION,

    /** Raised for a trip or an activity a student joined. */
    ACTIVITY,

    /** Raised as a late-payment charge on another invoice. */
    LATE_FEE,

    /** Typed in by a staff member with no other source record. */
    MANUAL,

    /** Anything the sources above do not cover. */
    OTHER
}
