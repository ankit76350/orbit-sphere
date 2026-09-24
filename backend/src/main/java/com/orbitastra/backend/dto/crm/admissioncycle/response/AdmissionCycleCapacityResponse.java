package com.orbitastra.backend.dto.crm.admissioncycle.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Endpoint #7 — <b>seats against reality</b>.
 *
 * <p><b>This is the counterpart to a decision #29 made on purpose.</b> #29 does not cap offers
 * against the seat table, because schools deliberately over-offer — sixty letters for forty places,
 * because a fifth of families go elsewhere. The justification written there was "counting offers
 * against places is #7's job", and until this existed <b>over-offering was invisible</b>: nothing
 * anywhere told a school it had promised more seats than it has.
 *
 * <p><b>The counts are computed, never stored.</b> Keeping them on {@code AdmissionCycle} would
 * make it a document every application write has to touch.
 */
public record AdmissionCycleCapacityResponse(

        String admissionCycleId,
        String name,
        String academicYear,
        String status,

        List<Row> classes,

        /** The same numbers added up, so a caller does not have to. */
        Row total,

        /**
         * How many classes have promised more seats than they have.
         *
         * <p><b>The headline.</b> A school reading one number wants this one — and zero is the
         * answer most of the time, which is what makes a non-zero worth noticing.
         */
        int overCommittedClasses,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /**
     * One configured class, or the total.
     *
     * <p><b>Only the classes the seat table lists.</b> A class nobody set seats for is not part of
     * this round — #17 refuses an application for it, so it cannot have applicants either.
     */
    public record Row(

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String classDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String className,

            /** What the school configured with #4. */
            int totalSeats,

            /** Held back — management quota, siblings, whatever the school keeps. */
            int reservedSeats,

            /** {@code totalSeats − reservedSeats}: what this round can actually give away. */
            int openSeats,

            /** Submitted, under review, or waiting on more information. Nobody has decided yet. */
            long pending,

            /** Approved and not yet offered a letter. */
            long approved,

            /** Held back, in the hope that a seat frees up. */
            long waitlisted,

            /** A letter is out with the family. */
            long offered,

            /** They said yes. The seat is spoken for. */
            long accepted,

            /** A child on the register. #33 is what sets it and is not built. */
            long enrolled,

            /** Refused by the school. */
            long rejected,

            /** The family pulled out, or declined the letter. */
            long withdrawn,

            /**
             * {@code offered + accepted + enrolled} — every seat this round has promised or given.
             *
             * <p><b>Approved is NOT committed.</b> A school that has approved forty children has
             * decided something; it has not promised anyone a seat until a letter goes out.
             */
            long committed,

            /**
             * {@code openSeats − committed}, and <b>it is allowed to go negative</b>.
             *
             * <p>That is the whole point of this endpoint. Clamping it at zero would hide the one
             * thing it exists to show — and a school reading "0 free" cannot tell "exactly full"
             * from "twenty over".
             */
            long freeSeats,

            /** {@code freeSeats < 0}. Said out loud so nobody has to notice a minus sign. */
            boolean overCommitted) {
    }
}
