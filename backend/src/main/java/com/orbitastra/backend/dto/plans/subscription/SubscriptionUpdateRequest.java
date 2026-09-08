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
