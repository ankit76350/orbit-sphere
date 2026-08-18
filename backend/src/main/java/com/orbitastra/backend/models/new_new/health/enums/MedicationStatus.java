package com.orbitastra.backend.models.new_new.health.enums;

/**
 * Whether a dose was actually given.
 *
 * <p>A dose that was not given is written down, not left out. If a child's
 * lunchtime medicine was missed, the record has to say so and say why, because a
 * missing row and a skipped dose look identical and only one of them needs a phone
 * call home.
 */
public enum MedicationStatus {
    /** Given as planned. */
    GIVEN,

    /** The child would not take it. */
    REFUSED_BY_STUDENT,

    /** Not given on purpose, with the reason recorded. */
    WITHHELD,

    /** Should have been given and was not. Nobody meant this to happen. */
    MISSED
}
