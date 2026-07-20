package com.orbitastra.backend.models.undone.feeengine.enums;

/**
 * Escalation ladder for fee-due reminders, in increasing order of urgency.
 */
public enum ReminderChannel {
    WHATSAPP,
    SMS,
    CALL,
    MEETING_LETTER
}
