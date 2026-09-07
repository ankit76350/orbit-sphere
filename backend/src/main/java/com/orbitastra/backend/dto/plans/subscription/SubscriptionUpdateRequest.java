package com.orbitastra.backend.dto.plans.subscription;

import java.time.Instant;

import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Edits what a school is contracted to. Endpoint #15.
 *
 * <p><b>This replaced extend-trial</b>, and took in #23 (auto-renew) and #24 (limit overrides)
 * with it — three endpoints that each moved one column of the same document, with three sets of
 * rules to keep in step and no way to correct two of them at once without two writes and two
 * history rows for one decision. One PATCH, one transaction, one history row. Pushing a trial end
 * date out is now {@code currentPeriodEnd} on this request.
 *
 * <p><b>It is the operator's override, not the ordinary path.</b> The lifecycle endpoints (#17 to
 * #22) each know one transition and its rules — renewing raises an invoice, suspending has a
 * reason, cancelling decides what happens to the money already paid. This asks no questions: it
 * writes what it is told. That is what makes it useful when a subscription is already wrong, and
 * why it should not be how a subscription is ordinarily renewed or cancelled.
 *
 * <h2>How PATCH behaves here</h2>
 *
 * <p>Absent and null look identical to Jackson, so every field has a defined absence:
 *
 * <pre>
 * field omitted, or null   -> leave it exactly as it is
 * field has a value        -> replace it
 * a nested block omitted   -> leave both of its fields alone
 * a nested block sent      -> replace both; null inside means "no value"
 * </pre>
 *
 * <p><b>Why one field is nested.</b> The two capacity overrides are nullable on the model, so
 * they can be cleared, and "omitted" has to be told apart from "cleared" — a block is the only
 * way to say it. Everything else here is {@code @NotNull} on the model and cannot be cleared at
 * all, so there is nothing to disambiguate and those stay flat — which is also what lets a
 * trial's end date be moved on its own.
 *
 * <h2>What is deliberately absent</h2>
 *
 * <p><b>Nothing that decides what the school pays.</b> {@code contractedPrice} and
 * {@code currencyCode} are #25; {@code billingCustomerReference} is #26; the plan is #16.
 * Changing a price changes what gets invoiced, and changing a currency changes what money it is
 * invoiced in — commercial decisions with a paper trail of their own. Behind the same request as
 * "push the trial out a fortnight", repricing a school would look like an administrative tidy-up.
 *
 * <p>So this endpoint covers <b>when</b> a subscription runs, <b>what state</b> it is in, and
 * <b>how much of the product</b> it may use. Nothing about the money.
 *
 * <p>{@code reasonForChanges} is not a field a caller sets either — it is written from
 * {@code reason} on every edit. Setting it directly would let somebody record an explanation for
 * a change they did not make.
 *
 * <p>{@code subscriptionNo} is the number the sequence handed out and the way this subscription
 * is addressed. Editing it would renumber a record that invoices and history rows already point
 * at, and leave a gap in the numbering that reads as a deleted subscription.
 *
 * <p>{@code current} is owned by a unique partial index on {@code {schoolId, current}}. Setting
 * it false leaves a school with no current subscription and every read answering 404; setting it
 * true on a second one is a duplicate-key error. Neither is an edit, and a school has one
 * subscription anyway, so there is nothing to switch between.
 *
 * <p>{@code schoolId} is in the URL. A PATCH that could move a subscription to another school
 * would name one school and mean another.
 */
public record SubscriptionUpdateRequest(

        /**
         * Example: SubscriptionStatus.SUSPENDED
         *
         * <p>Any of the six, with no transition rules applied — see the note above about this
         * being the override.
         *
         * <p><b>Moving to CANCELLED stamps no date.</b> When a subscription was cancelled is the
         * {@code effectiveAt} of its {@code CANCELLED} history row, which is written here anyway;
         * a second copy on the document could only ever disagree with it.
         */
        SubscriptionStatus status,

        /**
         * Example: BillingCycle.MONTHLY
         *
         * <p>Only the cycle. The period dates are not recalculated from it — an edit that
         * silently moved the period end would change what the school is billed for while
         * looking like a change of cadence.
         */
        BillingCycle billingCycle,

        /**
         * Example: 2026-04-01T00:00:00Z
         *
         * <p>Checked against the resulting {@code currentPeriodEnd}, whichever of the two moved,
         * so a period cannot be left running backwards by editing one end of it.
         */
        Instant currentPeriodStart,

        /**
         * Example: 2027-03-31T23:59:59Z
         *
         * <p><b>This is what extend-trial used to do.</b> Send it alone to push a trial or a paid
         * period out; nothing else changes.
         */
        Instant currentPeriodEnd,

        /** Example: false */
        Boolean autoRenew,

        /**
         * The negotiated capacity, replaced as a pair.
         *
         * <p>Omit the block to leave both alone. Send it to replace both, and send {@code null}
         * inside for either one to remove that override and fall back to the plan's own limit —
         * which is the only way to say "take this override away", since omitting the field is
         * how you say "leave it".
         */
        @Valid LimitOverrides limitOverrides,

        /**
         * Why. Example: "Renegotiated at renewal — 20% partner discount."
         *
         * <p><b>Stored in two places, because they answer different questions.</b> It goes on the
         * subscription as {@code reasonForChanges} — why it is the way it is now, readable without
         * a second query — and on the history row next to the list of fields that moved, which is
         * written whether or not a reason was given.
         *
         * <p><b>Every edit overwrites {@code reasonForChanges}, including with null.</b> A reason
         * left standing from an earlier edit would explain the wrong change. So sending no reason
         * is saying "this change has no recorded reason", not "keep the last one".
         *
         * <p>It is not a change in itself: a request carrying only a reason changes nothing and is
         * refused with {@code NO_CHANGES_REQUESTED} rather than storing an explanation for an
         * edit that did not happen.
         */
        @Size(max = 500) String reason) {

    /**
     * The pair. A null field inside means "no override", not "leave it alone".
     *
     * <p>Both are a count of things, so zero is not a limit anybody negotiated — an override that
     * permits nothing is refused rather than stored.
     */
    public record LimitOverrides(Long maxStudentsOverride, Long maxUsersOverride) {
    }

    /** True when the caller asked for nothing at all — answered with a 400, not a silent 200. */
    public boolean isEmpty() {
        return status == null && billingCycle == null && currentPeriodStart == null
                && currentPeriodEnd == null && autoRenew == null && limitOverrides == null;
    }
}
