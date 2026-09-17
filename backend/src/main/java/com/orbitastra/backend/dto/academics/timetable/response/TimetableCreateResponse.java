package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What #1 answers with when it has written a range.
 *
 * <h2>A summary, not every day it wrote</h2>
 *
 * <p>A fortnight of a 400-period school is 4,000 entries, and echoing them back would make the
 * response larger than the request that caused it — for data the caller already holds. So the dates
 * come back and the periods do not.
 *
 * <p><b>Except for a single date</b>, where {@code timetable} carries the day in full. That is the
 * common call — one day, written and then rendered — and making it a second request to see what was
 * just created would be a round trip for something the server had in its hand. The field is absent
 * rather than null when a range was written, so the two cases are told apart by presence.
 *
 * <h2>Skipped dates are named</h2>
 *
 * <p>Holidays and weekly offs inside a range are <b>skipped, not refused</b>: any range longer than
 * about five days contains one, and refusing the whole request over it would make ranges useless.
 * What was skipped is listed, because a caller that asked for fourteen days and got ten has no
 * other way to learn why.
 */
public record TimetableCreateResponse(

        LocalDate startDate,
        LocalDate endDate,

        /** How many days were written. Never zero — nothing written at all is a refusal. */
        int createdCount,

        /** How many periods each written day carries. The same for every date in the range. */
        int entriesPerDay,

        /** Every date a timetable now exists for, in order. */
        List<LocalDate> createdDates,

        /** Every date in the range that was left alone, with the reason. */
        List<SkippedDateResponse> skippedDates,

        /** The day itself, present only when exactly one date was written. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        DailyTimetableResponse timetable,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
