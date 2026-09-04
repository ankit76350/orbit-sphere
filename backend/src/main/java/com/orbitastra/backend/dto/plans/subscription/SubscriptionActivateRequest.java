package com.orbitastra.backend.dto.plans.subscription;

import java.time.Instant;

import jakarta.validation.constraints.Size;

/**
 * What to send when a trial turns into a paying subscription. Endpoint #14.
 *
 * <p>Every field is optional, and the whole body is too. The ordinary case is "the trial ended
 * and they agreed to buy", which needs nothing said: the paid period starts now and runs for one
 * billing cycle. The fields are here for the case that is not ordinary.
 *
 * <p>The plan, the price and the limits are not in this list on purpose. This endpoint only
 * changes a trial into a paying subscription; changing what they are paying for is a different
 * endpoint, so it cannot be done here by accident.
 */
public record SubscriptionActivateRequest(

        /** When they start paying. Leave it out and it is now. */
        Instant currentPeriodStart,

        /**
         * When the first paid period ends. Leave it out and we work it out from the billing
         * cycle. A CUSTOM cycle has no set length, so there it has to be sent.
         */
        Instant currentPeriodEnd,

        /** Why, written on the history row so somebody can see later what happened. */
        @Size(max = 500) String reason) {

    /** An empty request, for when the caller sends no body at all. */
    public static SubscriptionActivateRequest empty() {
        return new SubscriptionActivateRequest(null, null, null);
    }
}
