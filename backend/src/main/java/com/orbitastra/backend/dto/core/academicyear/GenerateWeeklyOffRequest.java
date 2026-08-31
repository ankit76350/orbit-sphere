package com.orbitastra.backend.dto.core.academicyear;

import java.time.DayOfWeek;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Generates one weekday's non-working days across a year. Endpoint #23.
 *
 * <p><b>This endpoint is not a convenience — the model requires it.</b> There is no "weekly off
 * day" field anywhere in this system, deliberately: schools in this market may run on Sunday
 * with the off day on any other weekday, so nothing may infer a closure from the day of the
 * week. Every non-working day is a <b>dated</b> entry.
 *
 * <p>That is the right model and it has a direct consequence: a year needs roughly 52 dated
 * WEEKLY_OFF rows, and nobody is typing those in. Without this endpoint, either somebody enters
 * 52 dates by hand or a developer eventually hardcodes Sunday somewhere — the exact assumption
 * the model was designed to prevent.
 *
 * <p>{@code fromDate} and {@code toDate} default to the whole year. They exist for the school
 * that changes its off day mid-year, which is why the generator does not simply always cover
 * the full range.
 *
 * <p>Dates that already carry a holiday of any kind are <b>skipped, not overwritten</b>. A
 * Sunday that is also Diwali stays Diwali — the more specific reason is the more useful one, and
 * silently replacing it would lose information the school entered by hand.
 */
public record GenerateWeeklyOffRequest(

        /** Example: DayOfWeek.SUNDAY — accepts MONDAY through SUNDAY. */
        @NotNull DayOfWeek dayOfWeek,

        /** Defaults to the year's startDate. Example: 2026-04-01 */
        LocalDate fromDate,

        /** Defaults to the year's endDate. Example: 2027-03-31 */
        LocalDate toDate,

        /** What each generated entry is called. Defaults to "Weekly Off". */
        @Size(max = 120) String name) {

    public String nameOrDefault() {
        return name == null || name.isBlank() ? "Weekly Off" : name.trim();
    }
}
