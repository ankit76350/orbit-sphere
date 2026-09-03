package com.orbitastra.backend.dto.core.academicyear;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The whole calendar, for endpoint #20.
 *
 * <p><b>A wrapper around the list rather than a bare array.</b> The endpoint used to take
 * {@code [ … ]} directly, which reads more naturally but validates worse: Spring reports a bad
 * element of a bare array as a {@code HandlerMethodValidationException}, not the
 * {@code MethodArgumentNotValidException} every other endpoint here produces, and the caller got
 * a Java method signature and an error count instead of the field that was wrong. Wrapping the
 * list in an object makes it an ordinary request body again, so the same handler and the same
 * {@code fieldErrors} shape apply as everywhere else.
 *
 * <p>It also leaves somewhere to put a future option — a dry run, a "keep what is already there"
 * flag — without changing the shape of the request a second time.
 *
 * <p>{@code holidays} is required, and an empty list is the honest way to clear the calendar. A
 * body of {@code &#123;&#125;} is refused rather than treated as "clear it": wiping a year of
 * closures should not be what happens when a field is forgotten.
 */
public record HolidayCalendarRequest(

        /** One row per reason. Two rows may share a date; that is a day with two reasons. */
        @NotNull @Valid List<HolidayRequest> holidays) {
}
