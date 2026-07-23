package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    // References AcademicYear.name (unique per school), e.g. "2026-2027".
    // Optional: when omitted the server derives the year from startDate.
    private String academicYear;

    private LocalDate startDate;

    private LocalDate endDate;

    @Valid
    private List<ClassSectionTimetable> classTimetables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSectionTimetable {
        @NotBlank(message = "classDocsId is required")
        private String classDocsId;

        @NotBlank(message = "section is required")
        private String section;

        @Valid
        private List<TimetablePeriod> periods;
    }
}
