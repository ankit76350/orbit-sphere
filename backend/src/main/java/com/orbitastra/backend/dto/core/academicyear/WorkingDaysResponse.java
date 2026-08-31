package com.orbitastra.backend.dto.core.academicyear;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Every working day in a range, and how many there are. The answer to G10.
 *
 * <p>G9 asked about one date; this is the same question in bulk. Attendance percentages and fee
 * proration need a number, not two hundred individual lookups, and both need <b>the same</b>
 * number — so it is worked out here once rather than by each caller counting for itself.
 *
 * <p><b>The dates come back, not only the count.</b> A timetable being laid out, a fee schedule
 * being spread over teaching days, an attendance register being opened for a term — all of them
 * need to know <i>which</i> days, and the count alone would send every one of them back to G8 to
 * work it out again. Each day carries its {@code dayOfWeek} so a person reading the list does
 * not have to do calendar arithmetic to check it.
 *
 * <p><b>The range is echoed back.</b> {@code from} and {@code to} default to the year's own
 * dates when the caller leaves them off, so a bare call answers "which days does this year
 * actually teach on" — and its count is the denominator of every attendance percentage. A count
 * with no range beside it is a number somebody will later divide by the wrong thing.
 *
 * <p><b>Both ends are inclusive</b>, so {@code totalDayCount} counts the first and last day. And
 * {@code workingDayCount + closedDayCount == totalDayCount} always — {@code workingDayCount} is
 * the length of the list rather than a separate subtraction, so the number and the list cannot
 * disagree.
 *
 * <p><b>{@code closedDayCount} counts days, not reasons.</b> A Sunday that is also Diwali is one
 * closed day, not two. That is the same distinction
 * {@link HolidayCalendarResponse} draws between {@code closedDayCount} and {@code eventCount},
 * and getting it wrong here would quietly overstate closures on exactly the weeks a school has
 * festivals. There is no per-type breakdown for that reason: with several reasons on a day the
 * per-type numbers cannot add up to the day count. Ask G8 if you need the reasons.
 *
 * <p><b>Nothing here comes from the day of the week.</b> A day is working unless a dated entry on
 * the calendar closes it. Schools here may run on a Sunday and take the weekly off on another
 * day, so {@code dayOfWeek} on each row is for a person to read and never something to filter on
 * — see G9.
 */
public record WorkingDaysResponse(
        String academicYearName,
        LocalDate from,
        LocalDate to,
        int totalDayCount,
        int workingDayCount,
        int closedDayCount,
        List<WorkingDay> workingDays) {

    /**
     * One day the school is open.
     *
     * <p>Only the date and the weekday. There is nothing else to say about a working day — the
     * reasons belong to the closed ones, and those are G8's.
     */
    public record WorkingDay(LocalDate date, DayOfWeek dayOfWeek) {

        public static WorkingDay on(LocalDate date) {
            return new WorkingDay(date, date.getDayOfWeek());
        }
    }

    /**
     * Takes the working days that were found and derives every count from them, so the numbers
     * and the list are always the same answer.
     */
    public static WorkingDaysResponse of(String academicYearName, LocalDate from, LocalDate to,
            int totalDayCount, List<WorkingDay> workingDays) {

        return new WorkingDaysResponse(
                academicYearName,
                from,
                to,
                totalDayCount,
                workingDays.size(),
                totalDayCount - workingDays.size(),
                workingDays);
    }
}
