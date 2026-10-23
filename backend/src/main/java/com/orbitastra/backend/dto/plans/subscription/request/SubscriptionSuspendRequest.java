package com.orbitastra.backend.dto.plans.subscription.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuts a school off for non-payment. Endpoint #19.
 *
 * <p><b>Why this is not a status edit.</b> #14 can write {@code SUSPENDED} into the status field,
 * and that is exactly the problem: cutting a school off stops its staff working, and a decision
 * that big should not look like the same request as pushing a date out. This endpoint knows one
 * transition, refuses everything that is not it, and makes the school's own access follow — none
 * of which #14 does or should do.
 *
 * <p><b>It moves two things, because one would not stop anything.</b> The subscription goes
 * {@code SUSPENDED}, which turns every feature off through #34's {@code allowed} flag; and the
 * school goes {@code SUSPENDED} too, which is what {@code CurrentSchoolResolver} reads to block
 * the tenant. Writing only the subscription would leave a school that had been "suspended" still
 * editing its own records.
 *
 * <p><b>What it does NOT stop</b>, and this is worth knowing before relying on it: nothing kills
 * the school's live sessions or halts its scheduled jobs, because neither exists yet. A user
 * already signed in is refused at the next request that checks, not thrown out mid-page. The
 * response says so rather than leaving it assumed.
 *
 * <p><b>Only a paying subscription can be suspended</b> — {@code ACTIVE} or {@code PAST_DUE}. A
 * trial is refused: there is no unpaid bill behind a trial, so cutting one off is not this
 * decision, and letting it through would leave #20 unable to say what resuming should restore.
 * {@code CANCELLED} and {@code EXPIRED} are finished, and one already {@code SUSPENDED} has
 * nothing to do.
 */
public record SubscriptionSuspendRequest(

        /**
         * Why the school is being cut off. <b>Required.</b>
         * Example: "Invoice INV/2026/08/000412 unpaid 30 days past the grace period."
         *
         * <p>Stored on the subscription as {@code reasonForChanges}, on the school as
         * {@code statusReason}, and on the history row. Three places because three people ask:
         * whoever looks at the subscription, whoever looks at the school wondering why it is
         * locked, and whoever audits the decision later.
         *
         * <p><b>Required, with no exception.</b> Everything else on this module takes a reason
         * because it changed a figure; this one takes a reason because it stopped a school
         * working. "The bill is unpaid" is not enough on its own — which bill, and how far past
         * the grace period, is what makes the decision answerable. Blank counts as missing.
         */
        @NotBlank @Size(max = 500) String reason) {
}
