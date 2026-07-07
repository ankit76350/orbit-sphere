package com.orbitastra.backend.models.core;

import java.time.DayOfWeek;
import java.time.LocalDate;

import com.orbitastra.backend.models.core.enums.HolidayType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDetail {

    private String name;

    private String description;

    private HolidayType type;

    // Used for PUBLIC_HOLIDAY, FESTIVAL, etc.
    private LocalDate date;

    // Used only when type == WEEKLY_OFF
    private DayOfWeek dayOfWeek;
}
