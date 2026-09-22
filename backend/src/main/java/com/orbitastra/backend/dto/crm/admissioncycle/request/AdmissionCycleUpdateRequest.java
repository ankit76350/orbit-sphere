package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.time.Instant;
import java.util.List;

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
 * <h2>Clearing needs its own list, and this is the first place in the project that is true</h2>
 *
 * <p>The convention elsewhere is {@code ""} clears and absent leaves alone, which works for a
 * String. <b>It cannot work for an {@code Instant}</b>: there is no empty instant, and a record
 * cannot tell an absent key from a {@code null} one — both arrive as null.
 *
 * <p>So {@link #clear} names the fields to empty:
 *
 * <pre>
 * { "notes": "" }                            clears the notes, the old way
 * { "clear": ["notes"] }                     clears the notes, the new way
 * </pre>
 *
 * <p>Both spellings work for {@code notes}, because the rest of the project uses the first and
 * this endpoint kept the second. Naming a field in {@code clear} <b>and</b> sending it a value is
 * a refusal rather than a guess — the request says two things and only one can be true.
 *
 * <p><b>The four dates are no longer clearable</b>, since 2026-09-22: they are required on create,
 * so emptying one would leave a cycle the create endpoint would not have made. The mechanism stays
 * because {@code notes} still needs it — and because an Instant still cannot be cleared with
 * {@code ""} if a future field wants to be.
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

        /** When the school starts taking enquiries. Moveable, but not clearable. */
        Instant inquiryOpenAt,

        /** When families can start applying. Moveable, but not clearable. */
        Instant applicationOpenAt,

        /** The last moment a form is taken. Moveable, but not clearable. */
        Instant applicationCloseAt,

        /** The last moment an offered family can enroll. Moveable, but not clearable. */
        Instant enrollmentDeadlineAt,

        /** Anything the school wants to remember. {@code ""} clears it, or name it in {@code clear}. */
        @Size(max = 2000) String notes,

        /**
         * The fields to empty. <b>Only {@code notes}.</b>
         *
         * <p><b>The four dates were removed from this list on 2026-09-22</b>, when they became
         * required on create. A cycle with no application window is one #17 cannot check a form
         * against, so emptying one would leave the cycle in a state the create endpoint would
         * refuse to make. Send a different date instead of clearing it.
         *
         * <p>An unknown name here is a refusal rather than being ignored — a caller who misspells
         * it should be told, not left believing something was removed.
         */
        List<String> clear) {

    /** Never null, so callers do not have to check. */
    public List<String> safeClear() {
        return clear == null ? List.of() : clear;
    }
}
