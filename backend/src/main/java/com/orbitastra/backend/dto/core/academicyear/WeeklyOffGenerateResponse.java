package com.orbitastra.backend.dto.core.academicyear;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * What the weekly-off generator did. Endpoint #23.
 *
 * <p>Its own record rather than the shared calendar response, because the two numbers that
 * matter here do not exist anywhere else: how many were created, and how many dates were
 * <b>skipped because they already had a holiday</b>.
 *
 * <p>That second number is the one to read. A generator reporting only "52 created" hides the
 * case where a school ran it twice, or where a third of the Sundays were already festivals. The
 * skipped dates are listed, not just counted, so the school can see what took precedence.
 *
 * <p>Running it again is safe and produces {@code generated: 0} with everything skipped — the
 * same shape as any other idempotent operation in this API.
 */
public record WeeklyOffGenerateResponse(
        String academicYearName,
        DayOfWeek dayOfWeek,
        LocalDate fromDate,
        LocalDate toDate,
        int generated,
        int skippedAlreadyHoliday,
        List<LocalDate> skippedDates,
        int holidayCountAfter,
        String changeSummary) {
}
