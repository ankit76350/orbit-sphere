package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
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

        /**
         * What to do next. A <b>write</b> field — it says what just happened.
         *
         * <p>Left out of the JSON when it is null, which is what the reads pass. A read did not
         * change anything, so it has nothing to say about what happens next, and a
         * {@code "nextStep": null} sitting on every row of a list is noise a client then has to
         * decide whether to trust. Every write sets it, so nothing about #18 to #27 changes.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /**
     * The same year, for a read. Used by G5.
     *
     * <p>No {@code nextStep}: nothing just happened, so there is nothing to say about what to do
     * next. The field drops out of the JSON rather than coming back null.
     */
    public static AcademicYearResponse fromAcademicYear(AcademicYear year) {
        return fromAcademicYear(year, null);
    }

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
