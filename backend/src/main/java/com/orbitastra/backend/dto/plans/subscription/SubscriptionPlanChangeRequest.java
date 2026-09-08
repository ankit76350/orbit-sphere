package com.orbitastra.backend.dto.plans.subscription;

import java.math.BigDecimal;
import java.time.Instant;


import com.orbitastra.backend.models.plans.enums.BillingCycle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Moves a school onto a different plan, or a newer version of the one it is on. Endpoint #16.
 *
 * <p><b>The change takes effect immediately.</b> There is no "from the next period" option and no
 * field asking for one: a subscription holds one plan, not a current one and a pending one, so a
 * scheduled change would have nowhere to live. Moving the pointer now and calling it next period
 * would hand the school its new entitlements early, so the endpoint does the honest thing —
 * the plan changes when the request is made, and the period restarts with it.
 *
 * <p><b>It asks nothing about the money already paid, and moves none.</b> The school is part-way
 * through a period it has paid for, and this endpoint does not charge, credit or refund any of
 * it — because nothing in this codebase raises an invoice at all:
 * {@code subscription_invoices} has no writer and #17 is not built. Deciding what should happen
 * to that money is a commercial question, and it belongs with whatever eventually raises the
 * invoice rather than with the request that moves the plan.
 *
 * <p><b>Why this is not #14.</b> #14 edits the terms of the plan a school is already on. This
 * moves it to a different plan, which changes what the school is entitled to, what it costs and
 * how often it is billed — so #14 deliberately cannot touch the plan pointer, and this endpoint
 * deliberately cannot touch anything #14 owns.
 *
 * <h2>What follows the plan, and what survives it</h2>
 *
 * <p><b>A finished subscription is how a school comes back.</b> No status is refused. Moving a
 * {@code CANCELLED} or {@code EXPIRED} subscription onto a plan opens the new row at
 * {@code ACTIVE} and renewing again, because carrying a cancellation onto a plan somebody has
 * just bought would sell a school something it cannot use.
 *
 * <pre>
 * from the new plan   -> currencyCode
 * from the new plan   -> billingCycle, the price and both ceilings, unless this request names them
 * from this request   -> currentPeriodStart, which is required
 * kept as it is       -> autoRenew, unless this request names it
 * kept as it is       -> status, billingCustomerReference, subscriptionNo, current
 * </pre>
 *
 * <p>Two kinds of absence, and the difference matters: a price or a ceiling left out takes the
 * <b>new plan's</b> figure, because those are terms agreed against a plan. {@code autoRenew} left
 * out keeps <b>the school's</b> existing setting, because a plan has no opinion about renewal.
 *
 * <p><b>The new plan is the starting point for everything negotiable</b>, and this request is how
 * a negotiated figure is carried across. That is deliberate: a price and a ceiling are agreed
 * against a particular plan, and moving a school to a different plan means the terms are being
 * renegotiated whether or not anybody says so. Copying the old figures over silently would be
 * this endpoint inventing terms nobody agreed to; requiring them to be re-stated makes the new
 * arrangement somebody's decision.
 */
public record SubscriptionPlanChangeRequest(

        /**
         * The plan family to move to, max 40. Example: "PREMIUM"
         *
         * <p>Named by code and version rather than by id, the same way every plan URL names one.
         * It may be the plan the school is already on — that is how a school moves to a newer
         * version of it — but not the same code <i>and</i> version, which would change nothing.
         */
        @NotBlank @Size(max = 40) String planCode,

        /** Which version of it. Example: 2 */
        @NotNull Integer planVersion,

        /**
         * What this school pays on the new plan. Example: 44999.00
         *
         * <p><b>Absent means the new plan's list price</b> — which is the ordinary case, and the
         * reason this field is optional while the two decisions above are not. Send one to carry
         * a negotiated price across, or to negotiate a new one as part of the move. Zero is
         * allowed; negative is refused.
         *
         * <p>A school on a discount does <b>not</b> keep it automatically. A discount is agreed
         * against a plan at a price, and the new plan has a different price — so carrying the old
         * figure over silently would be inventing a deal nobody made. Sending it explicitly is
         * how the same arrangement is continued.
         */
        BigDecimal contractedPrice,

        /**
         * How often the school is billed on the new plan. Example: BillingCycle.QUARTERLY
         *
         * <p><b>Absent means the new plan's own cycle</b>, which is the ordinary move. Send one to
         * put the school on the new plan at a different cadence — the same negotiation #13 allows
         * on a sale, and stored on the subscription rather than on the plan.
         *
         * <p><b>It is this cycle that decides the new period</b>, and therefore whether
         * {@code currentPeriodEnd} is required. Moving to a {@code YEARLY} plan but billing
         * {@code CUSTOM} means an end date has to be sent; moving to a {@code CUSTOM} plan but
         * billing {@code MONTHLY} means one is derived and none is needed.
         */
        BillingCycle billingCycle,

        /**
         * When the new period starts. <b>Required.</b> Example: 2026-10-01T00:00:00Z
         *
         * <p><b>Today or later</b>, never the past: {@code 400 PERIOD_START_IN_PAST}. Nothing here
         * invoices a period, so a backdated start would record a school as paying for time nothing
         * could ever charge it for. The comparison is against the start of today in the school's
         * own timezone.
         *
         * <p><b>It used to be derived and is now stated.</b> The start is the anchor the new
         * period's end is measured from, and it is also the instant the row being left stops
         * serving — so it decides two dates rather than one, which makes it somebody's decision.
         *
         * <p><b>Sending a future date does not delay the plan change.</b> The plan moves when this
         * request is made; what a later start moves is when the new plan's billing period begins.
         * The row being left has its end set to this same instant, so the two periods meet
         * exactly and the school is never on neither.
         */
        @NotNull Instant currentPeriodStart,

        /**
         * When the new period ends. Example: 2027-06-30T23:59:59Z
         *
         * <p><b>Absent means the cycle decides</b> — 30, 90, 180 or 365 days from
         * {@code currentPeriodStart}. Required when the cadence is {@code CUSTOM}, which has no
         * length: absent there is {@code 400 BILLING_PERIOD_END_REQUIRED}, the same refusal #13
         * and #14 give.
         *
         * <p>The cadence that matters is {@code billingCycle} on this request where one was sent,
         * and the <b>new</b> plan's otherwise — never the old plan's. A school moving from a
         * yearly plan to a monthly one gets a 30-day period.
         *
         * <p>It has to be after {@code currentPeriodStart}, or
         * {@code 400 INVALID_BILLING_PERIOD}.
         */
        Instant currentPeriodEnd,

        /**
         * A negotiated student ceiling on the new plan. Example: 2500
         *
         * <p><b>Absent copies the new plan's {@code maxStudents}</b>, exactly as #13 does on a
         * sale. So a plan change leaves the school on the new plan's own ceiling unless this
         * request names another — including when it had negotiated one on the plan it is
         * leaving. A negotiated ceiling is agreed against a plan, and this is a different plan,
         * so carrying it across silently would be inventing a term nobody agreed.
         *
         * <p>At least 1. Zero is refused: there is nothing to remove here, and a ceiling of no
         * students is not one anybody negotiated. Removing an override afterwards is #14, where
         * zero means exactly that.
         */
        Long maxStudentsOverride,

        /**
         * A negotiated user ceiling on the new plan. Example: 300
         *
         * <p>The same: absent copies the new plan's {@code maxUsers}, and at least 1 when sent.
         */
        Long maxUsersOverride,

        /**
         * Whether the subscription renews itself at the end of the new period. Example: false
         *
         * <p><b>Absent leaves it exactly as it is</b>, which is the one absence on this request
         * that does <i>not</i> mean "take the new plan's". A plan has no opinion about renewal —
         * it is the school's standing instruction, and a school that turned auto-renewal off has
         * not changed its mind by moving plan. Defaulting to {@code true} here, as #13 does on a
         * sale, would switch it back on for exactly the school that had asked for it off.
         *
         * <p>Nothing acts on it. #17 starts the next period when it is called and does not
         * consult this flag, because nothing calls #17 on a schedule — so there is no automatic
         * renewal for the flag to switch off. What it does do is show up on the school's own
         * billing view, which tells the school its subscription does not renew automatically.
         */
        Boolean autoRenew,

        /**
         * Why. <b>Required.</b> Example: "Upgraded at renewal — outgrew Starter's 500 students."
         *
         * <p>Stored on the subscription as {@code reasonForChanges} and on the history row next to
         * the plan it moved from. A plan change moves what a school is entitled to and what it
         * pays; an unexplained one is the hardest kind of record to answer questions about
         * afterwards. Blank counts as missing.
         */
        @NotBlank @Size(max = 500) String reason) {
}
