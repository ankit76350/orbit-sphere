package com.orbitastra.backend.models.new_new.core.embedded;

import java.time.LocalDate;

import com.orbitastra.backend.models.new_new.core.enums.HolidayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One dated holiday embedded inside {@code AcademicYear.holidays}.
 *
 * <p>This value object is not a collection and has no id or schoolId. Weekly
 * offs are stored as individual dated occurrences so the school can remove or
 * convert one specific occurrence into a working day without changing every
 * other weekly off.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDetail {

    // Example: "Diwali"
    @NotBlank
    private String name;

    // Example: "School closed for the Diwali festival"
    private String description;

    // Example: HolidayType.FESTIVAL
    @NotNull
    private HolidayType type;

    // Example: 2026-11-08
    @NotNull
    private LocalDate date;
}
