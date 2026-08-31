package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;

/**
 * A year's whole holiday calendar. Shared by every calendar endpoint.
 *
 * <p>One record for all six rather than one per verb. Every calendar operation — replace, add,
 * edit, remove one, remove a type — leaves the caller wanting the same thing: what does the
 * calendar look like now. Returning only the changed entry would make a form re-fetch to redraw,
 * and six near-identical records would drift the first time somebody added a field to one.
 *
 * <p>{@code changeSummary} is what distinguishes them. It says what this particular call did —
 * "added Diwali on 2026-11-08", "removed 52 WEEKLY_OFF entries" — so a `200` with an unchanged
 * count is still legible.
 *
 * <p>{@code countsByType} is here because the interesting question about a calendar is almost
 * never how many entries it has. It is whether the weekly offs were generated, and whether the
 * festival list has been entered yet. A single total cannot answer either.
 *
 * <p>Holidays come back <b>sorted by date</b>. They are stored in whatever order they were added,
 * and an unsorted calendar is unreadable.
 */
public record HolidayCalendarResponse(
        String academicYearName,
        LocalDate startDate,
        LocalDate endDate,
        int holidayCount,
        Map<HolidayType, Integer> countsByType,
        List<HolidayView> holidays,
        String changeSummary) {

    public static HolidayCalendarResponse fromAcademicYear(AcademicYear year, String changeSummary) {
        List<HolidayDetail> stored = year.getHolidays() == null ? List.of() : year.getHolidays();

        List<HolidayView> sorted = stored.stream()
                .sorted(Comparator.comparing(HolidayDetail::getDate))
                .map(HolidayView::fromDetail)
                .toList();

        // LinkedHashMap so the enum's own order survives into the JSON, which makes two
        // responses comparable by eye.
        Map<HolidayType, Integer> counts = new LinkedHashMap<>();
        for (HolidayType type : HolidayType.values()) {
            long n = stored.stream().filter(h -> h.getType() == type).count();
            if (n > 0) {
                counts.put(type, (int) n);
            }
        }

        return new HolidayCalendarResponse(
                year.getName(),
                year.getStartDate(),
                year.getEndDate(),
                stored.size(),
                counts,
                sorted,
                changeSummary);
    }
}
