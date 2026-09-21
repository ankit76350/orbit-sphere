package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The seat table for one admission cycle. Endpoint #4.
 *
 * <h2>A PUT, because the table is one thing</h2>
 *
 * <p>Whoever sets intake reads the whole table and rewrites it: twelve classes, a number against
 * each. A per-row {@code PATCH} would need a row identity, and {@link
 * com.orbitastra.backend.models.crm.embedded.IntakeCapacity} has none — it is embedded, with no id
 * of its own. The class id is the only thing that identifies a row, and keying a write on it would
 * make "rename this row's class" impossible to express.
 *
 * <p><b>So sending a shorter list removes the rows you left out.</b> That is what replacing means,
 * and it is the one thing to be careful of: a caller who sends one row has set the table to one
 * row, not added one.
 *
 * <h2>An empty list is how the table is cleared</h2>
 *
 * <p>{@code {"capacities": []}} is a valid request and empties it. That is a real thing to want —
 * a school that set seats against the wrong classes wants them gone, not zeroed — and it is why
 * the list is required but may be empty.
 */
public record AdmissionCycleCapacitiesRequest(

        /**
         * The version the table was read at. Optional, and honoured when sent.
         *
         * <p>Matters more here than on #2, because this write REPLACES: two people setting intake
         * from two stale screens is one of them silently losing every row the other added. Send it
         * and the second gets {@code 409 CONCURRENT_MODIFICATION}.
         */
        Long version,

        /**
         * One row per class. <b>Required, but may be empty</b> — an empty list clears the table.
         *
         * <p>Capped so a request cannot carry an unbounded array. A school with more than two
         * hundred classes in one year has a different problem than this endpoint.
         */
        @NotNull @Size(max = 200) List<@Valid Seat> capacities) {

    /**
     * One class's seats.
     *
     * <p><b>No id of its own</b>, because the stored row has none: {@code IntakeCapacity} is
     * embedded and the class is what identifies it. That is also why a class may appear only once
     * in the list — two rows for one class is a request with two answers.
     */
    public record Seat(

            /**
             * Which class. Example: "67aa15d9dc3f7d0011111111"
             *
             * <p>Must be a class of <b>the cycle's academic year</b>, not merely of this school. A
             * cycle admits into one year, and seats against another year's class would be seats
             * nobody could ever fill.
             */
            @NotBlank @Size(max = 60) String classDocsId,

            /**
             * How many seats the class is offering. Example: 60
             *
             * <p>Zero is allowed and is not the same as leaving the class out: it says the school
             * considered this class and is offering nothing, which is what a closed grade looks
             * like.
             */
            @NotNull @Min(0) Integer totalSeats,

            /**
             * How many of those are held back. Example: 10
             *
             * <p>Defaults to 0. Cannot exceed {@code totalSeats} — reserving more than exist is
             * not a number a school could act on.
             */
            @Min(0) Integer reservedSeats) {
    }
}
