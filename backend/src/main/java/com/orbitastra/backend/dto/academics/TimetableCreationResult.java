package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What a timetable creation actually did: which dates were written and which were skipped and why. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableCreationResult {
    private int daysCreated;

    private int totalEntries;

    private List<LocalDate> dates;

    private List<SkippedDate> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedDate {
        private LocalDate date;

        // e.g. "Independence Day", "Sunday (weekly off)"
        private String reason;
    }
}
