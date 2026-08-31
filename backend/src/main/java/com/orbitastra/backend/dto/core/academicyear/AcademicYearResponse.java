package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;

import com.orbitastra.backend.models.core.AcademicYear;

/**
 * An academic year as it now stands.
 *
 * <p>{@code current} is <b>derived here, never stored</b>. AcademicYear has no such field on
 * purpose: two sources for "which year is it" is two sources that can disagree, and the dates
 * are already authoritative. That is also why endpoint #18 refuses overlapping years — if two
 * years covered one date, this flag would be true for both and every "which year is this?"
 * lookup would have two answers.
 *
 * <p>{@code holidayCount} rather than the holidays themselves. A full year carries roughly
 * sixty entries, most of them generated weekly offs, and returning all of them on every create
 * or date change would bury the fields that actually changed. The calendar has its own
 * endpoints — #20 to #23.
 */
public record AcademicYearResponse(
        String academicYearId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        long durationDays,
        boolean current,
        int holidayCount,
        Boolean enrollmentEnabled,
        Boolean resultsLocked,
        String nextStep) {

    public static AcademicYearResponse fromAcademicYear(AcademicYear year, String nextStep) {
        LocalDate today = LocalDate.now();
        boolean isCurrent = !today.isBefore(year.getStartDate()) && !today.isAfter(year.getEndDate());

        return new AcademicYearResponse(
                year.getId(),
                year.getName(),
                year.getStartDate(),
                year.getEndDate(),
                year.getStartDate().until(year.getEndDate()).getDays() >= 0
                        ? java.time.temporal.ChronoUnit.DAYS.between(
                                year.getStartDate(), year.getEndDate()) + 1
                        : 0,
                isCurrent,
                year.getHolidays() == null ? 0 : year.getHolidays().size(),
                year.getEnrollmentEnabled(),
                year.getResultsLocked(),
                nextStep);
    }
}
