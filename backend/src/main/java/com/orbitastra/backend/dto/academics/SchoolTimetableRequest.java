package com.orbitastra.backend.dto.academics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Creates weekly timetables for many class sections of one school in a single
 * call. Every entry's periods are repeated on every day in "days" (defaults
 * to MONDAY-FRIDAY), valid from "effectiveFrom" until "effectiveTo" (null =
 * ongoing). Teacher clashes are checked across all entries and against the
 * stored slots; nothing is saved unless the whole batch is conflict free.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolTimetableRequest {
    private String schoolId;

    private List<DayOfWeek> days;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private List<ClassSectionTimetable> classTimetables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSectionTimetable {
        private String classId;

        private String section;

        private List<TimetablePeriod> periods;
    }
}
