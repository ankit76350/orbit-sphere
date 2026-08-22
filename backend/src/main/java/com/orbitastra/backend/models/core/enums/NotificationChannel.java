package com.orbitastra.backend.models.new_new.core.enums;

/**
 * Delivery channel supported by communication and notification features.
 * User-specific preferences and provider delivery records belong to their own
 * communication models.
 */
public enum NotificationChannel {
    /** Mobile or browser push notification. */
    PUSH,

    /** Email delivery. */
    EMAIL,

    /** SMS delivery. */
    SMS,

    /** WhatsApp business-message delivery. */
    WHATSAPP
}
