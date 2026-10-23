package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One date in the range that no timetable was written for, and why.
 *
 * <p><b>Skipping is reported, never silent.</b> A caller asking for a fortnight and receiving ten
 * days has to be able to see that the missing four were the weekly offs and Independence Day —
 * otherwise the only way to find out is to read every date back one at a time.
 */
public record SkippedDateResponse(

        LocalDate date,

        /** {@code NOT_A_WORKING_DAY} today; the field exists so a second reason can be added. */
        String reason,

        /** What the school called it — "Independence Day", "Weekly off". */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String holidayName) {
}
