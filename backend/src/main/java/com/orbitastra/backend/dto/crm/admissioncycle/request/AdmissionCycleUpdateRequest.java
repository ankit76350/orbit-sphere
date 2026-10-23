package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * <h2>{@code questions} IS here, and it replaces the whole list — added 2026-09-30</h2>
 *
 * <p>Absent leaves the questions alone. An empty list clears them. Anything else is the round's
 * questions <b>as they will be</b>: a question left out of the list is removed.
 *
 * <p><b>But the ids survive, and that is the whole point.</b> Send a question back with the
 * {@code id} it already has and it keeps that id while its wording and {@code required} change.
 * Send one with no id and it is added with a new one. That is what lets a school fix a typo in a
 * question without orphaning every answer already given — an answer is stored under the id, so a
 * replace that minted new ids would quietly break every application in the round.
 *
 * <p><b>The name cannot be blanked.</b> A cycle needs one: it is the only thing telling two rounds
 * of the same year apart. Sending {@code ""} is a refusal, not a clear.
 */
public record AdmissionCycleUpdateRequest(

        /**
         * The version the correction was decided against. <b>Required since 2026-09-30.</b>
         *
         * <p>A cycle somebody else has changed since answers {@code 409 CONCURRENT_MODIFICATION}
         * instead of the change being applied over their work. Leaving it out is
         * {@code 400 VALIDATION_FAILED}: a caller who cannot say what they read cannot be told
         * their read was stale.
         */
        @NotNull Long version,

        /**
         * A new name for the round. Still has to be free within the year.
         *
         * <p>Keeping the name it already has is fine — the check compares ids, not names, so a
         * request that sends the current name back is not a clash with itself.
         */
        @Size(max = 120) String name,

        /** When families can start applying. Moveable; there is no way to empty it. */
        Instant applicationOpenAt,

        /** The last moment a form is taken. Moveable; there is no way to empty it. */
        Instant applicationCloseAt,

        /** Anything the school wants to remember. {@code ""} clears it. */
        @Size(max = 2000) String notes,

        /**
         * The round's questions as they should end up. Absent leaves them alone; {@code []}
         * clears them; anything else replaces the list.
         *
         * <p>The order sent is the order they are asked in, so reordering is done by sending them
         * in the new order.
         */
        @Size(max = 200) List<@Valid Question> questions) {

    /**
     * One question on the form, as it should end up.
     *
     * <p><b>{@code id} is how an existing question is kept.</b> Send the id a question already
     * has and that question is edited in place; leave it out and a new question is added. An id
     * that this cycle does not have is a refusal rather than a new question, because it almost
     * always means the caller is editing a round they did not read.
     */
    public record Question(

            /**
             * The id of a question this cycle already has, or absent for a new one.
             * Example: "6abd14a14ea41d2ce449b27f"
             */
            @Size(max = 60) String id,

            /** What the family reads on the form. */
            @NotBlank @Size(max = 500) String question,

            /** Whether the family has to answer it. Absent means not required. */
            Boolean required) {
    }
}
