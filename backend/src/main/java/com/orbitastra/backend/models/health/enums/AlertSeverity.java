package com.orbitastra.backend.models.health.enums;

/**
 * How serious an alert is.
 *
 * <p>This decides what a teacher sees first. A child with a nut allergy that can
 * kill them and a child who cannot eat onions both have an ALLERGY alert, and
 * showing those two the same way is how the important one gets missed.
 *
 * <p>LIFE_THREATENING is not a label to hand out freely. It means somebody could
 * die today without the right action, and it should be shown on every screen that
 * names the child.
 */
public enum AlertSeverity {
    /** Worth knowing, no action needed day to day. */
    LOW,

    /** Needs watching, and staff should know before a trip or sports. */
    MODERATE,

    /** Needs a plan, and the staff around the child must all know. */
    HIGH,

    /** Could kill without the right action. Shown wherever the child is named. */
    LIFE_THREATENING
}
