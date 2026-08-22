package com.orbitastra.backend.models.health.enums;

/**
 * How a visit to the school nurse ended.
 *
 * <p>This is the field a parent asks about, and the one that decides whether
 * anybody else needs telling. Everything from SENT_HOME downwards means a guardian
 * has to be contacted.
 */
public enum ClinicVisitOutcome {
    /** Sorted out and back to lessons. */
    RETURNED_TO_CLASS,

    /** Kept in the clinic for a while, then back to lessons. */
    RESTED_IN_CLINIC,

    /** A guardian came and took the child home. */
    SENT_HOME,

    /** Sent on to a doctor or a hospital, with a guardian. */
    REFERRED_TO_HOSPITAL,

    /** An ambulance was called. */
    EMERGENCY_SERVICES,

    /** Seen, and being kept an eye on for now. */
    UNDER_OBSERVATION
}
