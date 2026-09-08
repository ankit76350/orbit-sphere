package com.orbitastra.backend.dto.plans.subscription;

import java.time.Instant;

import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edits what a school is contracted to. Endpoint #14.
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
 * an override sent as 0    -> remove it, and fall back to the plan's own limit
 * </pre>
 *
 * <p><b>One exception, and it is the period end.</b> {@code currentPeriodEnd} is not only replaced
 * when it is sent — it is <i>recalculated</i> when {@code billingCycle} or
 * {@code currentPeriodStart} moves and no explicit end came with them, because the cycle is what
 * decides how long a period runs. It then appears in the changed-field list and the history row
 * like any other edit, so a derived change is never a silent one.
 *
 * <p>{@code reason} is the exception: it is not optional, and it is not one of the fields being
 * edited. See its own note.
 *
 * <p><b>Every field here is flat, and the two overrides pay a small price for it.</b> Jackson
 * cannot tell a field that was omitted from one sent as {@code null} — both arrive as
 * {@code null} — so for the two nullable fields, "leave it alone" and "take it away" would be the
 * same request. Zero carries the second meaning instead, which works because zero cannot mean
 * anything else: nobody negotiates a ceiling of no students. The rest of the fields are
 * {@code @NotNull} on the model and cannot be cleared at all, so the question never arises for
 * them.
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
 * {@code reason} on every edit, and {@code reason} is <b>required</b>, so no edit here can go
 * unexplained. Setting it directly would let somebody record an explanation for a change they
 * did not make.
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
         * <p><b>The cycle decides the period, so changing it moves {@code currentPeriodEnd}</b> —
         * recalculated as {@code currentPeriodStart} plus the new cycle's days, and reported in
         * the changed-field list like any other edit. It has to: an end derived as "start + 365"
         * is not the end of a MONTHLY period, so leaving it would bill the school for a year while
         * the document says it pays monthly.
         *
         * <p>Send {@code currentPeriodEnd} alongside to say the date yourself; an explicit one
         * always wins, exactly as on #13.
         *
         * <p><b>Moving to {@code CUSTOM} requires {@code currentPeriodEnd} with it</b>, because
         * CUSTOM has no length to derive from and the stored date belongs to the cadence being
         * left: {@code 400 BILLING_PERIOD_END_REQUIRED}. Moving <i>away</i> from CUSTOM needs
         * nothing extra — the new cycle's length settles it.
         */
        BillingCycle billingCycle,

        /**
         * Example: 2026-04-01T00:00:00Z
         *
         * <p>Checked against the resulting {@code currentPeriodEnd}, whichever of the two moved,
         * so a period cannot be left running backwards by editing one end of it.
         *
         * <p><b>Required whenever {@code billingCycle} is sent.</b> A period is measured from
         * its start, so setting the cadence without saying when the period it describes begins
         * would leave this endpoint deriving an end from an anchor nobody restated:
         * {@code 400 PERIOD_START_REQUIRED}. Every other edit still leaves it alone when absent.
         *
         * <p><b>A new start cannot be in the past</b> — today or later, or
         * {@code 400 PERIOD_START_IN_PAST}. The <b>stored</b> start is not checked: a
         * subscription sold months ago has one in the past by definition, and refusing to edit
         * that would make every other field on this request unreachable for a running
         * subscription. Only a value being set now is a decision to refuse.
         *
         * <p><b>Moving the start moves the end with it</b> on the four fixed cycles, since the
         * end is the start plus the cycle's days — a period that kept its old end would be a
         * different length from the cadence the school is paying on. Send
         * {@code currentPeriodEnd} to override that. On a {@code CUSTOM} cycle the end stays put:
         * it is a date somebody agreed rather than a derivation.
         */
        Instant currentPeriodStart,

        /**
         * Example: 2027-03-31T23:59:59Z
         *
         * <p><b>This is what extend-trial used to do.</b> Send it alone to push a trial or a paid
         * period out; nothing else changes — sent alone, nothing is derived, because neither the
         * cycle nor the start moved.
         *
         * <p><b>An explicit date always wins.</b> Send it with a new {@code billingCycle} or a new
         * {@code currentPeriodStart} to say the end yourself instead of taking the derived one.
         * Required when moving to a {@code CUSTOM} cycle, which has no length to derive from.
         */
        Instant currentPeriodEnd,

        /** Example: false */
        Boolean autoRenew,

        /**
         * A negotiated student ceiling for this one school. Example: 2500
         *
         * <p>Omitted leaves whatever is there. A number replaces it.
         *
         * <p><b>Zero removes it</b>, so the school falls back to the plan's own
         * {@code maxStudents}. That needs saying because it is the one thing a flat, nullable
         * field cannot say for itself: Jackson hands over {@code null} both for a field that was
         * omitted and for one sent as {@code null}, so "leave it alone" and "take it away" arrive
         * identical. Zero is free to mean the second because it cannot mean anything else — a
         * school permitted zero students is not a limit anybody negotiated.
         *
         * <p>A negative number is still refused. It is a typo, not an instruction.
         */
        Long maxStudentsOverride,

        /**
         * A negotiated user ceiling for this one school. Example: 300
         *
         * <p>Same three rules: omitted leaves it, a number replaces it, zero removes it and falls
         * back to the plan's own {@code maxUsers}.
         */
        Long maxUsersOverride,

        /**
         * Why. <b>Required.</b> Example: "Renegotiated at renewal — 20% partner discount."
         *
         * <p><b>The only field on this request that must be sent.</b> Everything else here edits
         * a subscription somebody is paying for — its status, its dates, its capacity — and an
         * unexplained change to any of it is one nobody can answer for later. Blank counts as
         * missing, so {@code "  "} is refused too.
         *
         * <p><b>Stored in two places, because they answer different questions.</b> It goes on the
         * subscription as {@code reasonForChanges} — why it is the way it is now, readable without
         * a second query — and on the history row next to the list of fields that moved.
         *
         * <p><b>Every edit overwrites {@code reasonForChanges}.</b> A reason left standing from an
         * earlier edit would explain the wrong change. Because this field is required, an edited
         * subscription always carries a reason; null there means nothing has ever edited it.
         *
         * <p>It is not a change in itself. A request carrying only a reason changes nothing and is
         * refused with {@code NO_CHANGES_REQUESTED}, and a request whose fields all already hold
         * their values stores no reason either — an explanation for an edit that did not happen is
         * not worth keeping.
         */
        @NotBlank @Size(max = 500) String reason) {

    /** True when the caller asked for nothing at all — answered with a 400, not a silent 200. */
    public boolean isEmpty() {
        return status == null && billingCycle == null && currentPeriodStart == null
                && currentPeriodEnd == null && autoRenew == null && maxStudentsOverride == null
                && maxUsersOverride == null;
    }
}
