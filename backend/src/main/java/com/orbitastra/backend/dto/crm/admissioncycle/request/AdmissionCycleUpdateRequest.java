package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.time.Instant;

import jakarta.validation.constraints.Size;

/**
 * What one admission cycle may be corrected to. Endpoint #2.
 *
 * <h2>Every field is optional, and absent means "leave it"</h2>
 *
 * <p>Only what moved is sent. A school correcting the application close date should not have to
 * send the name and the other three dates back unchanged — that is three more chances to get one
 * of them wrong, and it turns a correction into a replacement.
 *
 * <h2>Clearing is {@code ""}, the same as everywhere else</h2>
 *
 * <p>{@code { "notes": "" }} empties the notes; leaving the field out keeps them. That is the
 * project's one convention, and #9 and #18 use it too.
 *
 * <p><b>This endpoint used to carry a {@code clear} list as well</b>, naming the fields to empty.
 * It was removed on 2026-09-25 because it had nothing left to do: the four dates came off it in
 * September when they became required on create, leaving {@code notes} — which {@code ""} already
 * cleared. Two spellings for one action is two things to document, two to test, and a refusal to
 * raise for callers who sent both.
 *
 * <p><b>An {@code Instant} still cannot be cleared with {@code ""}</b>, which is what the list was
 * originally for. Nothing needs that today; a field that does can bring the mechanism back.
 *
 * <h2>What is NOT here, and why</h2>
 *
 * <p><b>{@code academicYear}.</b> Moving a cycle to another year changes which cycles its name has
 * to be unique against, and changes the year every application under it is implicitly for. That is
 * not a correction; it is a different cycle. Create one.
 *
 * <p><b>{@code status}.</b> Its moves have preconditions and side effects that a field edit cannot
 * express — that is #3.
 *
 * <p><b>{@code capacities}.</b> The seat table is read and rewritten as a unit by whoever sets
 * intake, which is #4.
 *
 * <p><b>The name cannot be blanked.</b> A cycle needs one: it is the only thing telling two rounds
 * of the same year apart. Sending {@code ""} is a refusal, not a clear.
 */
public record AdmissionCycleUpdateRequest(

        /**
         * The version the correction was decided against. Optional, and honoured when sent.
         *
         * <p>Send it when the edit came from a screen that might be stale, and a cycle somebody
         * else has changed since answers {@code 409 CONCURRENT_MODIFICATION} instead of the change
         * being applied over their work. Leave it out and last write wins.
         */
        Long version,

        /**
         * A new name for the round. Still has to be free within the year.
         *
         * <p>Keeping the name it already has is fine — the check compares ids, not names, so a
         * request that sends the current name back is not a clash with itself.
         */
        @Size(max = 120) String name,

        /** When the school starts taking enquiries. Moveable; there is no way to empty it. */
        Instant inquiryOpenAt,

        /** When families can start applying. Moveable; there is no way to empty it. */
        Instant applicationOpenAt,

        /** The last moment a form is taken. Moveable; there is no way to empty it. */
        Instant applicationCloseAt,

        /** The last moment an offered family can enroll. Moveable; there is no way to empty it. */
        Instant enrollmentDeadlineAt,

        /** Anything the school wants to remember. {@code ""} clears it. */
        @Size(max = 2000) String notes) {
}
