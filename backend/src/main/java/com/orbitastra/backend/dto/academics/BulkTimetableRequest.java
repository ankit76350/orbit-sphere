package com.orbitastra.backend.dto.academics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Creates the weekly timetable for a single class section: the given periods
 * are repeated on every day in "days" (defaults to MONDAY-FRIDAY), valid from
 * "effectiveFrom" until "effectiveTo" (null = ongoing).
 *
 * For many class sections at once use {@link SchoolTimetableRequest}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTimetableRequest {
    private String schoolId;

    private String classId;

    private String section;

    private List<DayOfWeek> days;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private List<TimetablePeriod> periods;
}
