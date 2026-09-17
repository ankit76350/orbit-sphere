package com.orbitastra.backend.dto.academics.timetable.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A day's periods, written across one date or a range of them. Endpoint #1.
 *
 * <h2>The range is the whole point</h2>
 *
 * <p>A school does not build one Tuesday; it builds a pattern and applies it to a term. Sending
 * {@code startDate} alone writes one day; sending {@code endDate} too writes <b>every date from the
 * first to the last inclusive</b>, each with the same set of periods.
 *
 * <p><b>The dates are here rather than in the path</b>, which is a change from the plan's
 * {@code POST /timetables/{date}}. A date in the path and a range in the body would be two sources
 * for one fact, and the first request that disagreed with itself would have no right answer.
 *
 * <h2>Non-working days are skipped, not refused</h2>
 *
 * <p>Any range longer than about five days contains a weekly off, so refusing the whole request
 * because one date is a holiday would make ranges useless. Holidays are skipped and <b>named in the
 * response</b>. A range in which <i>every</i> date is a holiday writes nothing and is refused —
 * there is no partial success to report.
 *
 * <h2>Dates that already have a timetable refuse the whole request</h2>
 *
 * <p>All of it or none of it. A partial write across a range leaves a school unable to tell which
 * days were built from which request, and the refusal names every colliding date so the caller can
 * narrow the range rather than guess.
 */
public record DailyTimetableCreateRequest(

        /** The first date to write. Required. */
        @NotNull LocalDate startDate,

        /**
         * The last date to write, inclusive.
         *
         * <p><b>Absent means "just {@code startDate}"</b> — one day, which is the single-date form
         * of this endpoint. Equal to {@code startDate} means the same thing.
         */
        LocalDate endDate,

        /**
         * Every period of the day, applied to each date in the range.
         *
         * <p><b>At least one.</b> A day with no periods is not a day — it is the absence of a
         * document, which is already what a holiday looks like.
         *
         * <p>Capped at 4,000, which no real school approaches: a 300-section school at 12 periods
         * is 3,600, and the largest school in this database has 11 sections. The cap is there to
         * stop one request loading an unbounded list into memory, not to express a rule about
         * schools.
         */
        @NotEmpty(message = "a day needs at least one period")
        @Size(max = 4000, message = "a day cannot hold more than 4000 periods")
        @Valid
        List<TimetableEntryRequest> entries) {

    /** The last date this request covers — {@code startDate} when no range was given. */
    public LocalDate resolvedEndDate() {
        return endDate == null ? startDate : endDate;
    }
}
