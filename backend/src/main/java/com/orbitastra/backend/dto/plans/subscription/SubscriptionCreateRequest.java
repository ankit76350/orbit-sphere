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

        // Period end
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
