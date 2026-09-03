package com.orbitastra.backend.dto.plans.subscription;

import java.math.BigDecimal;
import java.time.Instant;

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

        /** When the first billing period starts. Defaults to now. Example: 2026-04-01T00:00:00Z */
        Instant currentPeriodStart,

        /**
         * When it ends. Worked out from the plan's billing cycle when omitted — a yearly plan
         * starting 1 April ends 31 March.
         *
         * <p><b>Required for a {@code CUSTOM} cycle</b>, which by definition has no length to
         * calculate. Example: 2027-03-31T23:59:59Z
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
