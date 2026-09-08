package com.orbitastra.backend.dto.plans.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import com.orbitastra.backend.models.plans.enums.BillingCycle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Puts a school on a plan for the first time. Endpoint #13.
 *
 * <p>The plan is named the way the rest of this module names one — <b>code and version</b>, not
 * a document id. That pair is a plan's business identity, it is what appears in every URL, and
 * asking a caller for a Mongo id would mean reading it out of another response first.
 *
 * <p><b>Most of this is optional, because the plan already knows it.</b> Price, currency and
 * billing cycle come from the plan version unless the caller deliberately says otherwise, which
 * is what a negotiated deal is. Sending nothing but the plan is the ordinary case and gives the
 * school exactly what the price list says.
 */
public record SubscriptionCreateRequest(

        /** Example: "PREMIUM" */
        @NotBlank @Size(max = 40) String planCode,

        /** Example: 1 */
        @NotNull Integer planVersion,

        /**
         * True to start the school on a trial rather than as a paying customer. Defaults to
         * false. Example: true
         *
         * <p>A boolean rather than the status itself, so {@code CANCELLED} cannot be the state a
         * subscription is created in. Only two starting states make sense and this picks between
         * them.
         */
        Boolean trial,

        /**
         * How often this school is billed. Example: BillingCycle.QUARTERLY
         *
         * <p><b>Absent means the plan's own cycle</b>, which is the ordinary sale — the plan says
         * how it is sold and nobody has to restate it.
         *
         * <p>Send one to sell the same plan on a different cadence, which is a real negotiation:
         * a school that wants to pay quarterly for a plan listed yearly is buying the same
         * entitlements on different terms. It is stored on the subscription rather than on the
         * plan, so it changes what this school is billed and nothing about the plan itself.
         *
         * <p><b>It is this cycle, not the plan's, that decides the period</b> — and that decides
         * whether {@code currentPeriodEnd} is required. Selling a {@code YEARLY} plan as
         * {@code CUSTOM} means an end date has to be sent; selling a {@code CUSTOM} plan as
         * {@code MONTHLY} means one is derived and none is needed.
         */
        BillingCycle billingCycle,

        /**
         * When the first billing period starts. Defaults to <b>the start of today in the school's
         * own timezone</b>, which is the ordinary sale. Example: 2026-04-01T00:00:00Z
         *
         * <p>Today rather than the instant the request landed, because a billing period is a pair
         * of dates somebody reads: "your year runs from the 7th" is what they expect to see, not
         * "from 12:47 on the 7th". Send one only for a contract that was agreed to start on some
         * other day — a backdated sale, or one that begins next quarter.
         */
        Instant currentPeriodStart,

        /**
         * When it ends. Worked out from the billing cycle when omitted — a yearly period starting
         * 1 April ends 365 days later.
         *
         * <p><b>Required when the cycle is {@code CUSTOM}</b>, which by definition has no length
         * to calculate: {@code 400 BILLING_PERIOD_END_REQUIRED} without it. That is the cycle on
         * <i>this request</i> where one was sent, and the plan's otherwise — so a plan listed
         * {@code CUSTOM} sold as {@code MONTHLY} needs no end date, and a {@code YEARLY} plan sold
         * as {@code CUSTOM} does. Example: 2027-03-31T23:59:59Z
         *
         * <p>Sending one on a fixed cycle overrides the derived date. It has to be after
         * {@code currentPeriodStart}, or {@code 400 INVALID_BILLING_PERIOD}.
         */
        Instant currentPeriodEnd,

        /** Defaults to true. Example: false */
        Boolean autoRenew,

        /**
         * What this school actually pays. Defaults to the plan's list price. Example: 45000.00
         *
         * <p>A separate field from the plan's price on purpose: this is the number that gets
         * invoiced, and a discount agreed with one school must not change the price list.
         */
        BigDecimal contractedPrice,

        /** A higher student ceiling than the plan's, for this school only. Example: 2500 */
        Long maxStudentsOverride,

        /** A higher user ceiling than the plan's, for this school only. Example: 300 */
        Long maxUsersOverride,

        /** The customer id at the payment provider, if there is one. Example: "cus_Qx7B2mR9" */
        @Size(max = 120) String billingCustomerReference,

        /**
         * Why this subscription was created, for the history row. Example: "Signed annual
         * contract, 10% partner discount."
         *
         * <p>Optional here and deliberately not required: creating a subscription is the
         * ordinary path, and the history row already records what happened and when. A reason
         * matters most on the operations that are <i>not</i> ordinary.
         */
        @Size(max = 500) String reason) {
}
