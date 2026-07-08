package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Creates the daily timetables of one school between startDate ("timetable
 * starts from") and endDate ("runs until", defaults to startDate). Every
 * class-section entry's periods are stored on every date in the range except
 * the school's holidays and weekly offs (from the holiday calendar) — no
 * document is created for those, and the response reports what was skipped.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableCreateRequest {
    private String schoolId;

    private LocalDate startDate;

    private LocalDate endDate;

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
