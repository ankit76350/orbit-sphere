package com.orbitastra.backend.dto.plans.subscription.request;

import java.time.Instant;

/**
 * Starts the next billing period. Endpoint #17.
 *
 * <p><b>Every field is optional, and the ordinary renewal sends no body at all.</b> A renewal is
 * the same plan at the same price for the next period: the plan, the version, the price, the
 * currency, both capacity ceilings, the cycle, {@code autoRenew} and the billing customer
 * reference all carry across untouched, negotiated ones included. Anything that could change one
 * of those would make this a change rather than a renewal, and changes have their own endpoints —
 * #14 for the terms, #16 for the plan.
 *
 * <p>So this request exists for exactly one reason: a {@code CUSTOM} cycle has no length, and
 * nothing can work out when its next period should end. On the other four cadences there is
 * nothing to send — an end date is refused, because the cadence already decides it.
 *
 * <p><b>The body may be omitted entirely.</b> {@code POST} with no body at all is the normal call
 * and is what the controller expects for the four fixed cycles; the request arrives as
 * {@code null} and is read as "derive the period from the cycle".
 */
public record SubscriptionRenewRequest(

        /**
         * When the next period ends. Example: 2027-06-30T23:59:59Z
         *
         * <p><b>Required when the subscription bills on a {@code CUSTOM} cycle</b>, and optional
         * for every other. A custom contract runs to a date somebody agreed rather than for a
         * fixed number of days, so there is nothing to derive — leaving it out is
         * {@code 400 BILLING_PERIOD_END_REQUIRED}, the same refusal #13 and #16 give for the same
         * missing figure.
         *
         * <p><b>Absent on a MONTHLY, QUARTERLY, HALF_YEARLY or YEARLY cycle means the cycle
         * decides</b> — 30, 90, 180 or 365 days from where the last period ended. That is the
         * ordinary renewal, and it is why no body is needed for it.
         *
         * <p><b>Sending one on a fixed cycle is refused</b> — {@code 400
         * BILLING_PERIOD_END_NOT_ALLOWED}, exactly as on #13, #14 and #16. Those four cadences
         * ARE their length, so a renewal that ran to a date the cadence disagreed with would
         * bill the school for a period its own record denies. A renewal renews; changing how
         * long a period runs is #14's job, or a change of cadence.
         *
         * <p>On CUSTOM it has to be after the new period's start, which is the day the previous
         * period ended — otherwise {@code 400 INVALID_BILLING_PERIOD}. A renewal that ended
         * before it began would be a period no school was ever on.
         */
        Instant currentPeriodEnd) {

    /** True when there is nothing to read — no body was sent, or one with no fields set. */
    public static boolean isEmpty(SubscriptionRenewRequest request) {
        return request == null || request.currentPeriodEnd() == null;
    }
}
