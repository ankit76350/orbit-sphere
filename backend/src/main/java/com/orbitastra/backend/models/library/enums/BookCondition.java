package com.orbitastra.backend.models.library.enums;

/**
 * What state a physical copy is in.
 *
 * <p>Recorded when a copy arrives and checked again every time it comes back, so a
 * book that is falling apart is noticed by the librarian rather than by the next child
 * who borrows it.
 */
public enum BookCondition {
    /** Never lent out yet. */
    NEW,

    /** Lent out and come back fine. */
    GOOD,

    /** Worn but still readable and lendable. */
    FAIR,

    /** Barely holding together. Should be repaired or withdrawn. */
    POOR
}
