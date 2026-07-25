package com.orbitastra.backend.models.undone.celebrations.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayNotificationSettings {

    /**
     * Send push notifications.
     */
    @Builder.Default
    private Boolean pushEnabled = true;

    /**
     * Send email notifications.
     */
    @Builder.Default
    private Boolean emailEnabled = false;

    /**
     * Send SMS notifications.
     */
    @Builder.Default
    private Boolean smsEnabled = false;

    /**
     * Notify guardians about student birthdays.
     */
    @Builder.Default
    private Boolean guardianNotification = false;

    /**
     * Notify students.
     */
    @Builder.Default
    private Boolean studentNotification = true;

    /**
     * Notify staff members.
     */
    @Builder.Default
    private Boolean staffNotification = true;
}