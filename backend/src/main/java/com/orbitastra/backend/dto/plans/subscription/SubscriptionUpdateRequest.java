package com.orbitastra.backend.dto.plans.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edits what a school is contracted to. Endpoint #15.
 *
 * <p><b>This replaced extend-trial.</b> That endpoint moved one date, and four others
 * (#23 to #26) each moved one other field, so five endpoints existed to edit five columns of the
 * same document — five sets of rules to keep in step, and no way to correct two fields at once
 * without two writes and two history rows for one decision. This edits any of them, in one
 * transaction, with one history row. Pushing a trial end date out is now
 * {@code currentPeriodEnd} on this request.
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
 * field is ""              -> clear it (billingCustomerReference only)
 * field has a value        -> replace it
 * a nested block omitted   -> leave both of its fields alone
 * a nested block sent      -> replace both; null inside means "no value"
 * </pre>
 *
 * <p><b>Why some fields are nested and others are not.</b> A field that is nullable on the model
 * can be cleared, so "omitted" and "cleared" have to be told apart, and a block is the only way
 * to say it — that is {@code limitOverrides} and {@code cancellation}. A field that is
 * {@code @NotNull} on the model cannot be cleared at all, so there is nothing to disambiguate and
 * it stays flat — that is {@code currentPeriodStart} and {@code currentPeriodEnd}, which is also
 * what lets a trial's end date be moved on its own.
 *
 * <h2>What is deliberately absent</h2>
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
         * being the override. Two things follow automatically, because the alternative is a
         * document that contradicts itself:
         *
         * <ul>
         * <li>moving <b>to</b> CANCELLED stamps {@code cancelledAt} if the request did not,</li>
         * <li>moving <b>away from</b> CANCELLED clears {@code cancelledAt} and
         * {@code cancellationReason}, since a live subscription cancelled on a date is not a
         * state that means anything.</li>
         * </ul>
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

        /** Example: 39999.50. Zero is allowed — a free deal is a deal; negative is not. */
        BigDecimal contractedPrice,

        /**
         * ISO 4217. Example: "INR"
         *
         * <p>Editable here, unlike on #13 where it always comes from the plan, because a
         * subscription whose currency is already wrong has to be correctable. The response says
         * so when the result no longer matches the plan's currency rather than leaving it to be
         * found on an invoice.
         */
        @Size(max = 3) String currencyCode,

        /** Send "" to remove it. Example: "cus_Qx7B2mR9" */
        @Size(max = 120) String billingCustomerReference,

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
         * When and why it was cancelled, replaced as a pair.
         *
         * <p>Only meaningful next to each other: a cancellation date with no reason is an
         * unexplained one, and a reason with no date is a cancellation that never happened. Send
         * {@code null} inside either to clear it.
         *
         * <p>Setting {@code status} to CANCELLED fills the date in on its own, so this exists for
         * correcting a cancellation already recorded, or for recording the reason with it.
         */
        @Valid Cancellation cancellation,

        /**
         * Moves the school onto a different plan or version.
         *
         * <p><b>Nothing follows the plan automatically.</b> The price, cycle and currency stay
         * exactly as they are unless this request also changes them — because a school on a
         * negotiated price that got moved to the next version of its plan should keep the price
         * it negotiated, and guessing which of the two the caller meant would be wrong half the
         * time. The response names anything that no longer matches the new plan.
         *
         * <p>The plan still has to be one that can be sold today: a draft's price is not settled
         * and a retired plan was taken off the menu. A published plan that is not publicly
         * available is fine — that is a private quote.
         *
         * <p>This is not #16. Proration, and deciding what happens to money already paid for the
         * period, belong there; this moves the pointer.
         */
        @Valid PlanMove plan,

        /**
         * Why, for the history row. Example: "Renegotiated at renewal — 20% partner discount."
         *
         * <p>Not stored on the subscription. It goes on the audit row next to the list of fields
         * that actually changed, which is written whether or not a reason was given.
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

    /** The pair. A null field inside means "no value", not "leave it alone". */
    public record Cancellation(Instant cancelledAt, @Size(max = 500) String cancellationReason) {
    }

    /** Both together, because half a plan reference addresses nothing. */
    public record PlanMove(@NotBlank String planCode, @NotNull Integer planVersion) {
    }

    /** True when the caller asked for nothing at all — answered with a 400, not a silent 200. */
    public boolean isEmpty() {
        return status == null && billingCycle == null && currentPeriodStart == null
                && currentPeriodEnd == null && autoRenew == null && contractedPrice == null
                && currencyCode == null && billingCustomerReference == null
                && limitOverrides == null && cancellation == null && plan == null;
    }
}
