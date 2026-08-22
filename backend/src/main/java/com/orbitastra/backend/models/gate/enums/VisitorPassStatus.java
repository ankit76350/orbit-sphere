package com.orbitastra.backend.models.gate.enums;

/**
 * How far one visit has got.
 *
 * <p>CHECKED_IN is the state that matters most. Every pass sitting at CHECKED_IN
 * is somebody still inside the school, and that list is what gets read out during
 * a fire drill.
 */
public enum VisitorPassStatus {
    /** Booked in advance, not arrived yet. */
    EXPECTED,

    /** Arrived and inside the school now. */
    CHECKED_IN,

    /** Left the school. */
    CHECKED_OUT,

    /** Called off before the visit happened. */
    CANCELLED,

    /** Was expected and never turned up. */
    NO_SHOW,

    /** Ran past the time it was good for without being used. */
    EXPIRED
}
