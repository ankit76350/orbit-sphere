package com.orbitastra.backend.models.undone.a_working.feeengine.enums;

/**
 * Escalation ladder for fee-due reminders, in increasing order of urgency.
 */
public enum ReminderChannel {
    WHATSAPP,
    SMS,
    CALL,
    MEETING_LETTER
}
