package com.orbitastra.backend.models.health.enums;

/**
 * A student's blood group.
 *
 * <p>UNKNOWN is a real answer and is the default. A blank field and "nobody has
 * told us" look the same on a screen, and in an emergency the difference matters:
 * one means look again, the other means stop looking and ask the parent.
 */
public enum BloodGroup {
    A_POSITIVE,
    A_NEGATIVE,
    B_POSITIVE,
    B_NEGATIVE,
    AB_POSITIVE,
    AB_NEGATIVE,
    O_POSITIVE,
    O_NEGATIVE,

    /** Not recorded yet. */
    UNKNOWN
}
