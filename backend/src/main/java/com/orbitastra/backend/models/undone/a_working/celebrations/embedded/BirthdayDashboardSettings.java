package com.orbitastra.backend.models.undone.a_working.celebrations.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayDashboardSettings {

    /**
     * Show today's birthdays.
     */
    @Builder.Default
    private Boolean showToday = true;

    /**
     * Show upcoming birthdays.
     */
    @Builder.Default
    private Boolean showUpcoming = true;

    /**
     * Show age along with the birthday.
     */
    @Builder.Default
    private Boolean showAge = false;

    /**
     * Number of upcoming days to display.
     */
    @Builder.Default
    private Integer upcomingDays = 7;
}