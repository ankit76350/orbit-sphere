package com.orbitastra.backend.models.conduct.enums;

/**
 * How serious a case is.
 *
 * <p>This decides who deals with it and how fast. A child talking in class and a
 * child hitting another child are both conduct, and treating them the same way is how
 * a school either over-reacts to nothing or misses something that mattered.
 *
 * <p>SEVERE means somebody was hurt or is at risk, and it must be looked at by a
 * senior member of staff the same day. It is also the level at which the safeguarding
 * question has to be asked out loud rather than assumed.
 */
public enum ConductSeverity {
    /** Dealt with by the class teacher, on the spot. */
    MINOR,

    /** Needs recording and a word with the family. */
    MODERATE,

    /** Needs a senior member of staff and a meeting with the family. */
    SERIOUS,

    /** Somebody was hurt or is at risk. Same-day, senior, and possibly outside help. */
    SEVERE
}
