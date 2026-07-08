package com.orbitastra.backend.models.core;

import java.time.LocalDate;

import com.orbitastra.backend.models.core.enums.HolidayType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One holiday on one concrete date. Weekly offs (e.g. every Sunday) are also
 * stored as dated entries — one per occurrence, expanded by
 * POST /api/academic-years/{id}/weekly-offs — so a single date can be removed
 * later (e.g. a working Sunday before exams).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDetail {

    private String name;

    private String description;

    private HolidayType type;

    private LocalDate date;
}
