package com.orbitastra.backend.dto.academics.timetable.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What #6 answers with when it has built one day from another.
 *
 * <h2>Both dates, because a copy is about two days</h2>
 *
 * <p>A response naming only the day it wrote would leave the caller unable to tell, from the answer
 * alone, which Monday this Tuesday came from — which is the one thing worth recording about a
 * copied day.
 *
 * <h2>Copied and kept are counted separately</h2>
 *
 * <p>Without a merge they are {@code n} and {@code 0}. With one, {@code keptCount} is what the
 * target already had and {@code copiedCount} is what arrived — and the difference is the whole
 * reason merging re-runs every conflict check: a teacher free in both days separately can still be
 * in two places once they are put together.
 */
public record TimetableCopyResponse(

        /** The day that was built — the date in the path. */
        LocalDate date,

        /** The day it was built from. */
        LocalDate sourceDate,

        String dailyTimetableDocsId,

        /** What #2 would have to send back to replace this day. */
        Long version,

        /** True when the target already existed and the copied periods were added to it. */
        boolean merged,

        /** How many periods were copied across. Never zero — a copy that found nothing is refused. */
        int copiedCount,

        /** How many the target already had. Zero unless this was a merge. */
        int keptCount,

        /** The day in full, as it now stands, in the order the periods are stored. */
        DailyTimetableResponse timetable,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
