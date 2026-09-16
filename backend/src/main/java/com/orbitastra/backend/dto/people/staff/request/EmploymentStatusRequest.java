package com.orbitastra.backend.dto.people.staff.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.people.staff.enums.EmploymentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A change of employment status, with the reason it happened. Endpoint #18b.
 *
 * <h2>Why status left #18</h2>
 *
 * <p>#18 corrects what was typed wrong on a record. <b>A status change is not a correction, it is
 * something that happened to somebody</b> — they went on leave, they were suspended, they
 * retired — and the thing a school needs six months later is not the new value but <i>why</i>.
 * A field on a general PATCH cannot demand that; an endpoint can.
 *
 * <h2>The reason is required for five of the seven, and the enum decides</h2>
 *
 * <pre>
 * PROBATION      no reason needed   a normal start
 * ACTIVE         no reason needed   the ordinary state
 * ON_LEAVE       REQUIRED           how long, backfill, paid?
 * SUSPENDED      REQUIRED
 * NOTICE_PERIOD  REQUIRED           whose decision was it
 * TERMINATED     REQUIRED           and ends the employment
 * RETIRED        REQUIRED           and ends the employment
 * </pre>
 *
 * <p>The rule lives on {@link EmploymentStatus#requiresReason()} rather than in a list here, so a
 * status added later brings its own answer and nothing has to remember to update this.
 *
 * <h2>A terminal status ends the employment, in the same write</h2>
 *
 * <p>{@code TERMINATED} and {@code RETIRED} set {@code current = false} and {@code effectiveUntil}
 * as they are applied. <b>They have to.</b> A record that is terminal and current at once is the
 * contradiction the module plan's open item 2 describes and nothing in the model prevents — the
 * plan gives that pairing to #17, and this endpoint is where it now happens.
 *
 * <p><b>Which means this absorbed #17</b>, {@code POST /staff/{id}/separate}. Ending an employment
 * is one status change among seven, and two endpoints that both close a record are two chances to
 * close it differently.
 */
public record EmploymentStatusRequest(

        /** The status to move to. Moving to the one it already has is refused, not ignored. */
        @NotNull EmploymentStatus status,

        /**
         * Why. Required for every status whose {@code requiresReason()} is true.
         *
         * <p><b>Accepted on the other two as well.</b> "Returned from maternity leave" beside
         * {@code ACTIVE} is worth keeping, and refusing it would make a school throw away the one
         * sentence that explains the row above it.
         */
        @Size(max = 500) String reason,

        /**
         * The last day of employment. Only read when the status is terminal.
         *
         * <p><b>Absent means today.</b> A school recording a resignation the week after it
         * happened needs to say so, and one recording it as it happens should not have to.
         */
        LocalDate effectiveUntil) {
}
