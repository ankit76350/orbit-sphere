package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How a fee reminder was sent, in growing order of seriousness.
 *
 * <p>The order matters. FeeReminderLog moves a family down this list so the
 * next reminder is stronger than the last one, instead of sending the same
 * WhatsApp message over and over.
 */
public enum ReminderChannel {
    /** A WhatsApp message to the guardian. */
    WHATSAPP,

    /** A text message to the guardian. */
    SMS,

    /** An email to the guardian. */
    EMAIL,

    /** A phone call from the fee desk. */
    CALL,

    /** A printed letter sent home. */
    LETTER,

    /** A letter asking the guardian to come and meet the school. */
    MEETING_LETTER
}
