package com.orbitastra.backend.dto.crm.admissioncycle.request;

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
         * The version the decision was made against. Optional, and honoured when sent.
         *
         * <p>Sent → a cycle somebody else has moved since answers
         * {@code 409 CONCURRENT_MODIFICATION}. Absent → last write wins.
         */
        Long version) {
}
