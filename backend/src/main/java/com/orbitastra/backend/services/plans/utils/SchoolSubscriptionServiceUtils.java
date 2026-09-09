package com.orbitastra.backend.services.plans.utils;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * The shared bits the school's own subscription endpoints need.
 *
 * <p>Moved out of {@code SchoolSubscriptionService} so that file holds the endpoints and nothing
 * else, the same way {@link PlatformSubscriptionServiceUtils} did for the platform side. #35 to
 * #38 land in that service later and will bring helpers of their own.
 *
 * <p>A {@code @Component} with no dependencies of its own, so it matches its sibling rather than
 * being a static class somebody has to remember is different.
 *
 * <p>Every method says which endpoints use it, in the note above it.
 */
@Component
public class SchoolSubscriptionServiceUtils {

    /**
     * Why this subscription grants nothing, or null when it does.
     *
     * <p>One method, so "is this subscription live" has one answer. The reason is returned rather
     * than a bare boolean because a screen has to say which of these it is: "your subscription
     * was cancelled" and "your period ran out" lead the school to do different things.
          *
     * Used by:
     * - featureAccessFor()
     */
    public String whyNotActive(SchoolSubscription subscription, String zone) {
        SubscriptionStatus status = subscription.getStatus();

        // PAST_DUE still grants. An unpaid invoice is a conversation, not a reason to lock a
        // school out of its attendance register in the middle of the morning.
        if (status == SubscriptionStatus.SUSPENDED) {
            return "This subscription is suspended.";
        }
        // A CANCELLED subscription keeps granting until the period it was paid for runs out.
        // That is what #21's ordinary shape means: a school cancelling mid-month has bought that
        // month, and refusing it the same afternoon would be keeping its money and taking the
        // product away. Only once the period is over does the cancellation refuse anything — so
        // this falls through to the period check below rather than blocking on the status.
        //
        // #21's immediate shape works by trimming currentPeriodEnd to now, which is what makes
        // that check bite at once. No extra field says "cancelled but still running": the status
        // says cancelled and the dates say how long for.
        if (status == SubscriptionStatus.CANCELLED) {
            Instant cancelledEnd = subscription.getCurrentPeriodEnd();
            if (cancelledEnd != null && !cancelledEnd.isAfter(Instant.now())) {
                return "This subscription was cancelled, and its period ended on "
                        + Dates.readable(cancelledEnd, zone) + ".";
            }
        }
        if (status == SubscriptionStatus.EXPIRED) {
            return "This subscription has expired.";
        }

        Instant end = subscription.getCurrentPeriodEnd();
        if (end != null && !end.isAfter(Instant.now())) {
            return "The subscription period ended on " + Dates.readable(end, zone) + ".";
        }

        return null;
    }
}
