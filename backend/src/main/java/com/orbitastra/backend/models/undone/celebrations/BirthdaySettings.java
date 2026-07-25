package com.orbitastra.backend.models.undone.celebrations;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.celebrations.embedded.BirthdayCardSettings;
import com.orbitastra.backend.models.undone.celebrations.embedded.BirthdayDashboardSettings;
import com.orbitastra.backend.models.undone.celebrations.embedded.BirthdayNotificationSettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "birthday_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdaySettings extends SchoolBase {

    /**
     * Enables or disables the Birthday module.
     */
    
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Notification-related settings.
     */
    @Builder.Default
    private BirthdayNotificationSettings notifications =
            BirthdayNotificationSettings.builder().build();

    /**
     * Birthday card settings.
     */
    @Builder.Default
    private BirthdayCardSettings cards =
            BirthdayCardSettings.builder().build();

    /**
     * Dashboard display settings.
     */
    @Builder.Default
    private BirthdayDashboardSettings dashboard =
            BirthdayDashboardSettings.builder().build();

    /**
     * Default birthday wish shown in auto-generated cards.
     */
    @Builder.Default
    private String defaultWishMessage =
            "Happy Birthday! Wishing you a wonderful year ahead.";
}