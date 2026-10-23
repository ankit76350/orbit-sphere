package com.orbitastra.backend.dto.plans.subscription.request;

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

        // Plan code
        @NotBlank @Size(max = 40) String planCode,

        // Plan version
        @NotNull Integer planVersion,

        // Trial status
        Boolean trial,

        // Billing cycle
        BillingCycle billingCycle,

        // Period start
        @NotNull Instant currentPeriodStart,

        /**
         * When the first period ends. Example: 2027-06-30T23:59:59Z
         *
         * <p><b>ONLY A CUSTOM CADENCE TAKES AN END DATE.</b> The four fixed cycles ARE their
         * length, so they work their own end out and refuse one that is sent:
         *
         * <pre>
         * MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY -> leave it out; start + 30/90/180/365 days
         *                                            sending one is 400 BILLING_PERIOD_END_NOT_ALLOWED
         * CUSTOM                                  -> required; absent is 400 BILLING_PERIOD_END_REQUIRED
         * </pre>
         *
         * <p><b>Refused rather than quietly dropped.</b> An end date sent with a fixed cadence
         * either agrees with the derivation, in which case it said nothing, or disagrees with it
         * — and then the record contradicts itself: a subscription reading MONTHLY whose period
         * runs six months bills the school for half a year while the document says it pays every
         * month. Answering 200 with a different date than the one sent would be ignoring the
         * caller without telling them.
         *
         * <p><b>To move a fixed-cadence period end, move what it is measured FROM</b> — the
         * start, on #14 — or change the cadence. Both derive a new end.
         *
         * <p>The cadence that decides is {@code billingCycle} on this request where one was
         * sent, and the plan version's otherwise. So a YEARLY plan sold as CUSTOM needs a date,
         * and a CUSTOM plan sold as QUARTERLY refuses one — the rule follows what the school is
         * actually billed on, not what the plan is listed at.
         *
         * <p>On CUSTOM it has to be after {@code currentPeriodStart}, or
         * {@code 400 INVALID_BILLING_PERIOD}.
         */
        Instant currentPeriodEnd,

        // Auto renew
        Boolean autoRenew,

        // Contracted price
        BigDecimal contractedPrice,

        // Student limit
        Long maxStudentsOverride,

        // User limit
        Long maxUsersOverride,

        // Billing customer
        @Size(max = 120) String billingCustomerReference,

        // Reason
        @Size(max = 500) String reason) {
}
