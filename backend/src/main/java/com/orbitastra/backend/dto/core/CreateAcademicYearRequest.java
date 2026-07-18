package com.orbitastra.backend.dto.core;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.core.HolidayDetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for defining an academic year. Server-owned fields ({@code id}
 * and audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateAcademicYearRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "name is required")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<HolidayDetail> holidays;
}
