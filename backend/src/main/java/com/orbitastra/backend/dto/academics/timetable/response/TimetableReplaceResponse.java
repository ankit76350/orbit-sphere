package com.orbitastra.backend.dto.academics.timetable.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What #2 answers with when it has replaced a day.
 *
 * <h2>What changed, not just what is there now</h2>
 *
 * <p>A replace is the one write in this module that can <b>silently destroy</b> work: a period the
 * caller left out of the list is gone, and a caller that left it out by accident has no way to tell
 * from a response that only shows the new day. So the answer says which periods survived, which
 * were added, and — the one that matters — <b>which were removed, by id</b>.
 *
 * <p>That last list is the closest thing this endpoint has to an undo. The ids in it are what an
 * {@code AttendanceSession} may still be pointing at.
 *
 * <h2>The new version, so the next replace does not have to re-read</h2>
 *
 * <p>{@code version} is required on the request. Returning the one the write produced means a
 * caller correcting a day twice does not have to call #7 in between.
 */
public record TimetableReplaceResponse(

        String dailyTimetableDocsId,

        /** The version the day now has. What a following replace must send. */
        Long version,

        /** Every period of the day as it now stands, in the order they were sent. */
        DailyTimetableResponse timetable,

        /** How many periods kept the id they came in with — the ones that survived. */
        int keptCount,

        /** How many are new, and were given a generated id. */
        int addedCount,

        /**
         * The ids of periods that were in the day and are not any more.
         *
         * <p><b>Named rather than counted</b>, because this is the destructive half of the write.
         * An {@code AttendanceSession} carrying one of these now points at nothing — see open item
         * 4 of this module's plan, which is not settled and which this endpoint does not enforce.
         */
        List<String> removedEntryIds,

        /** Repeated on every response until permissions exist. Deliberately hard to miss. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
