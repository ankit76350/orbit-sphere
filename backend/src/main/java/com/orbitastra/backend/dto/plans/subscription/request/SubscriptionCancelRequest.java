package com.orbitastra.backend.dto.plans.subscription.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ends a subscription. Endpoint #21.
 *
 * <p><b>The school usually keeps working until the period it already paid for runs out.</b> That
 * is the default and the ordinary case: a school that cancels in the middle of a month it has paid
 * for has bought that month, and cutting it off the same afternoon would be keeping its money and
 * taking the product away.
 *
 * <p>So a cancellation has two shapes, and {@code immediate} picks between them:
 *
 * <pre>
 * immediate absent or false  -> the period is left alone; access runs to currentPeriodEnd
 * immediate true             -> currentPeriodEnd is trimmed to now; access stops at once
 * </pre>
 *
 * <p><b>The status goes {@code CANCELLED} either way</b>, because the contract is over either way
 * and that is simply true. What decides whether the school can still work is the <b>period</b>: a
 * cancelled subscription keeps granting until its {@code currentPeriodEnd} passes. So no field
 * says "cancelled but still running" — the status says cancelled, and the dates say how long for.
 * That is the same division of labour #16 uses when it closes the row a school leaves.
 *
 * <p><b>Nothing quietly undoes it, and no extra check was needed for that.</b> #17 already
 * refuses to renew a {@code CANCELLED} subscription and #20 already refuses to resume one.
 * Bringing the school back means selling it something new — #13 or #16.
 *
 * <p><b>What nothing does yet, and this is the honest gap.</b> Nothing marks a lapsed subscription
 * {@code EXPIRED}, so a cancellation that has served out its period reads {@code CANCELLED} with
 * {@code periodEnded: true} rather than {@code EXPIRED} — correct in every field, and still not
 * tidied away. <b>A job will close these.</b> There is no endpoint for it: #22 was dropped
 * because a period end passing is a date arriving, not a decision anybody makes.
 *
 * <p><b>It does not touch the school.</b> Unlike #19, which takes the school's access down with
 * it, cancelling a subscription is a commercial end and not a lock-out: the school stays
 * {@code ACTIVE} and keeps working exactly as long as its period says. Winding the tenant down is
 * core's business — {@code OFFBOARDING}, then {@code CLOSED} — and doing it here would make one
 * request mean two decisions.
 *
 * <p><b>No money moves, and none is asked about.</b> Nothing raises, credits or refunds an invoice
 * anywhere in this codebase, so an immediate cancellation keeps whatever the school paid for the
 * part of the period it is giving up. What <i>should</i> happen to that is a commercial question,
 * and it belongs with whatever eventually raises invoices.
 */
public record SubscriptionCancelRequest(

        /**
         * Why the subscription is ending. <b>Required.</b>
         * Example: "School closing at the end of the academic year — confirmed by email 2026-09-08."
         *
         * <p>Stored on the subscription as {@code reasonForChanges} and on the history row beside
         * the {@code CANCELLED} event.
         *
         * <p><b>Required, with no exception.</b> This is the one transition nothing undoes: #20
         * will not resume a cancelled subscription, and #17 will not renew one — bringing the
         * school back means selling it something new. A record of the end that does not say why
         * is the one nobody can answer for.
         */
        @NotBlank @Size(max = 500) String reason,

        /**
         * Whether to end it now instead of at the end of the paid period. Example: true
         *
         * <p><b>Absent or false is the ordinary cancellation</b>, and the one the school expects:
         * it keeps working to {@code currentPeriodEnd}, having paid for that time.
         *
         * <p>{@code true} trims {@code currentPeriodEnd} to now, which is what stops the
         * access: #34 refuses a cancelled subscription whose period has run out. Use it when the
         * school should not have the product for another day — a fraud case, or a contract
         * terminated rather than run out.
         *
         * <p>It does <b>not</b> refund the unused part of the period, because nothing here can.
         * And trimming the date overwrites what the school had paid for, so the history row's
         * reason records the original end — the only place it survives, which is where #16 puts
         * a superseded subscription number for the same reason.
         */
        Boolean immediate) {

    /** True when the caller asked for the immediate shape. Absent and null both mean scheduled. */
    public boolean isImmediate() {
        return Boolean.TRUE.equals(immediate);
    }
}
