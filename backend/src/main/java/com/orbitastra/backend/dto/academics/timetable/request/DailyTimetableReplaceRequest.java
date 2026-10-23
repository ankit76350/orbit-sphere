package com.orbitastra.backend.dto.academics.timetable.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The complete day, replacing whatever was there. Endpoint #2.
 *
 * <h2>The only full-document write this module has, and the one to reach for last</h2>
 *
 * <p>Every other write exists so that this one is not needed: #3 adds a period, #4 corrects one —
 * the substitution this module exists for — #5 removes one, and #6 copies a day. This endpoint
 * overwrites <b>everything</b>, including periods the caller may never have seen, and a period it
 * leaves out is gone. The module's own plan puts it last and says it may never be needed.
 *
 * <h2>{@code version} is required, and it is the reason this is safe at all</h2>
 *
 * <p>A targeted update touches one embedded entry and cannot lose somebody else's edit to another.
 * A replace can lose all of them. So the caller has to say which version of the day it is
 * replacing, and a day that moved on since it was read is
 * {@code 409 CONCURRENT_MODIFICATION} rather than a silent overwrite of the other clerk's work.
 * The version comes from #7 or from #1's echo; it is not something to guess.
 *
 * <h2>Entry ids are kept where they are sent and generated where they are not</h2>
 *
 * <p>A replace that keeps a period should keep its <b>identity</b>, so that an
 * {@code AttendanceSession} already pointing at it still points at it. Send the
 * {@code timetableEntryId} back for every period that is surviving; leave it off for one being
 * added. Periods whose ids are not sent back are <b>dropped</b>, and the response names them.
 */
public record DailyTimetableReplaceRequest(

        /**
         * The version of the day being replaced, as #7 or #1 reported it.
         *
         * <p><b>Required, unlike on every other write in this module.</b> A replace overwrites
         * entries the caller never saw, so "I am replacing version 4" is the only thing standing
         * between two clerks and one of them losing a morning's work.
         */
        @NotNull Long version,

        /**
         * Every period the day is to have afterwards. This is the complete list, not a patch.
         *
         * <p><b>At least one.</b> A day with no periods is not a day — it is the absence of a
         * document, which is already what a holiday looks like. There is no way to empty a day
         * through this endpoint, and that is deliberate.
         *
         * <p>Capped at 4,000, the same cap #1 carries and for the same reason: to stop one request
         * loading an unbounded list into memory, not to express a rule about schools.
         */
        @NotEmpty(message = "a day needs at least one period")
        @Size(max = 4000, message = "a day cannot hold more than 4000 periods")
        @Valid
        List<TimetableEntryReplaceRequest> entries) {
}
