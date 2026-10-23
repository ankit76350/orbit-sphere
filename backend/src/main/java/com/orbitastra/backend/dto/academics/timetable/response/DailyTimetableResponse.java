package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.academics.timetable.DailyTimetable;

/**
 * One school day in full — the shape #7 returns and #1 echoes for a single date.
 *
 * <p><b>{@code academicYear} is derived, never sent.</b> The date decides which year this is, so a
 * caller reading it back is reading what the server worked out rather than what it was told.
 *
 * <p><b>{@code version} is here because #2 requires it.</b> A full-day replace has to say which
 * version of the day it is overwriting, and a caller that has just created a day should not have to
 * read it back to learn one.
 */
public record DailyTimetableResponse(

        String dailyTimetableDocsId,
        LocalDate date,
        String academicYear,

        /** What #2 must send back to replace this day. */
        Long version,

        int entryCount,
        List<TimetableEntryResponse> entries) {

    public static DailyTimetableResponse of(DailyTimetable day) {
        List<TimetableEntryResponse> entries = day.getEntries() == null
                ? List.of()
                : day.getEntries().stream().map(TimetableEntryResponse::of).toList();

        return new DailyTimetableResponse(
                day.getId(),
                day.getDate(),
                day.getAcademicYear(),
                day.getVersion(),
                entries.size(),
                entries);
    }
}
