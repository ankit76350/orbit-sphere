package com.orbitastra.backend.dto.core;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.core.HolidayDetail;

import lombok.Data;

/**
 * Full-replacement payload for an academic year (PUT). The update replaces the
 * dates and holiday calendar. The {@code name} is intentionally not part of
 * this request because other data references it and it is immutable after
 * creation. Server-owned fields are not accepted here.
 */
@Data
public class UpdateAcademicYearRequest {

    private String schoolId;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<HolidayDetail> holidays;
}
