package com.orbitastra.backend.dto.core;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.core.HolidayDetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Full-replacement payload for an academic year (PUT). The update replaces the
 * dates and holiday calendar, so {@code name} is required. Note the service
 * rejects a change to {@code name} once the year is in use (other data
 * references it by name). Server-owned fields are not accepted here.
 */
@Data
public class UpdateAcademicYearRequest {

    private String schoolId;

    @NotBlank(message = "name is required")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<HolidayDetail> holidays;
}
