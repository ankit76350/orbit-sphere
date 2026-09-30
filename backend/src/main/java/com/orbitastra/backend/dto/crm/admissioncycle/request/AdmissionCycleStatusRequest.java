package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.time.Instant;

import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Where to move an admission cycle. Endpoint #3.
 *
 * <h2>A POST with a verb, not a PATCH of a field</h2>
 *
 * <p>Each move has its own preconditions — opening needs a seat table, and only some moves are
 * legal from where the cycle is. A {@code PATCH} that set {@code status} would be six endpoints
 * wearing one name, and the refusals would have to explain which one the caller meant.
 *
 * <h2>There is no reason field, and there should be</h2>
 *
 * <p>Cancelling a round is the one move worth recording a reason for, and
 * {@link com.orbitastra.backend.models.crm.AdmissionCycle} has nowhere to put one — only
 * {@code notes}, which is the school's own description of the round rather than a record of what
 * happened to it. Asking for a reason and then dropping it would be worse than not asking, so this
 * does not ask. A {@code cancellationReason} on the model would fix it.
 */
public record AdmissionCycleStatusRequest(

        /**
         * Where the cycle should end up. Example: OPEN
         *
         * <p>Must be a legal move from where it is now — see the graph in the module's README. The
         * refusal names both ends and lists what <i>is</i> reachable.
         */
        @NotNull AdmissionCycleStatus status,


        /**
         * The new last moment a form is taken. <b>Required when reopening, refused otherwise.</b>
         * Example: "2027-01-31T18:29:59Z"
         *
         * <p><b>Because closing the round already used that field.</b> Moving to CLOSED stamps
         * {@code applicationCloseAt} with the moment the button was pressed, so by the time a
         * school decides to take more applications the round's closing date is in the past.
         * Reopening without a new one would leave a round that closed before it opened, which the
         * order check refuses — the school would be told to go and use #2 first, for a decision it
         * is already making right here. <b>How much longer to take applications IS the reopen.</b>
         *
         * <p><b>Only on {@code CLOSED → OPEN}.</b> A first opening from {@code DRAFT} or
         * {@code SCHEDULED} keeps the date the school published, so sending one there is
         * {@code 400 CYCLE_CLOSE_DATE_NOT_ALLOWED} rather than a field edit smuggled into a verb.
         * The same reading gives #12 its {@code LOST_REASON_NOT_ALLOWED}.
         *
         * <p>It is checked like any other: not after the academic year ends, and forwards against
         * the opening date the move is about to stamp with now.
         */
        Instant applicationCloseAt,



        /**
         * The version the decision was made against. <b>Required since 2026-09-30.</b>
         *
         * <p>A cycle somebody else has moved since answers {@code 409 CONCURRENT_MODIFICATION}
         * rather than the move landing on top of theirs. Leaving it out is
         * {@code 400 VALIDATION_FAILED}.
         */
        @NotNull Long version


) {
}
