package com.orbitastra.backend.dto.academics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.academics.DailyTimetable.TimetableEntry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One date's entries for a class section or a teacher, filtered out of the school's daily timetables. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaySchedule {
    private LocalDate date;

    private DayOfWeek dayOfWeek;

    private List<TimetableEntry> entries;
}
