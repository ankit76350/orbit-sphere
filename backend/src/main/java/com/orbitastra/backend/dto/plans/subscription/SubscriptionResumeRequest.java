package com.orbitastra.backend.dto.plans.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Switches a school back on after it pays. Endpoint #20.
 *
 * <p>The exact reverse of #19, and only that: the subscription goes back to {@code ACTIVE} and the
 * school with it. Nothing else moves — not the plan, not the price, not the period. A suspension
 * pauses access; lifting it does not renegotiate anything.
 *
 * <p><b>It resumes to {@code ACTIVE}, not to whatever it was before.</b> That needs no lookup and
 * loses nothing, because #19 only ever suspends an {@code ACTIVE} or a {@code PAST_DUE}
 * subscription — and a school that has paid is not {@code PAST_DUE} any more. Refusing to suspend
 * a trial is what makes this simple: there is no case where resuming should land somewhere other
 * than {@code ACTIVE}.
 *
 * <p><b>The period is not extended.</b> A school suspended for three weeks comes back to the same
 * {@code currentPeriodEnd} it had, so it has paid for time it could not use. That is deliberately
 * not fixed here: crediting a period is a money decision, nothing in this codebase raises or
 * credits an invoice, and quietly moving the end date would be this endpoint inventing a refund.
 * The response says so. Pushing the date out, if that is what was agreed, is #14.
 *
 * <p><b>Only a {@code SUSPENDED} subscription can be resumed.</b> An {@code ACTIVE} one has
 * nothing to resume; a {@code CANCELLED} or {@code EXPIRED} one ended rather than paused, and
 * bringing it back would be selling a new period without saying so — that is #13 or #16.
 */
public record SubscriptionResumeRequest(

        /**
         * What lets the school back on. <b>Required.</b>
         * Example: "Invoice INV/2026/08/000412 paid in full on 2026-09-08."
         *
         * <p>Stored on the subscription as {@code reasonForChanges}, on the school as
         * {@code statusReason} — replacing the suspension's — and on the history row.
         *
         * <p><b>Required, the same as #19's.</b> A suspension and its lifting are a pair, and a
         * record that says exactly why a school was cut off but only "resumed" for why it came
         * back answers half the question. Naming the payment is what closes it. Blank counts as
         * missing.
         */
        @NotBlank @Size(max = 500) String reason) {
}
