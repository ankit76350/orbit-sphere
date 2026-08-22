package com.orbitastra.backend.models.conduct.enums;

/**
 * How one child was involved in an incident.
 *
 * <p>This is the most important enum in the package. An incident is not a list of
 * children who were all equally in trouble: a bullying case has a child doing it and a
 * child it was done to, and recording them the same way is how a school ends up
 * disciplining a victim.
 *
 * <p>The old {@code academics/DisciplineLog} had one student per row and no role at
 * all, so a fight became three unrelated rows with no way to tell who started it or
 * who was hurt.
 */
public enum ParticipantRole {
    /** Did the thing, or took part in it. */
    RESPONSIBLE,

    /** Had it done to them. Never disciplined for the same event. */
    AFFECTED,

    /** Saw it happen. */
    WITNESS,

    /** Was there, and it is not yet clear which of the above they are. */
    PRESENT
}
