package com.orbitastra.backend.dto.plans.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * The school's own billing screen. Endpoint #33.
 *
 * <p><b>This is deliberately shorter than what the platform sees</b>
 * ({@link SubscriptionDetailResponse}, #27), and the fields left out are the point of having two
 * types instead of sharing one:
 *
 * <ul>
 *   <li><b>No {@code planListPrice}.</b> A school on a negotiated price would be shown a number
 *       it is not paying — which is either a discount somebody then has to explain or an
 *       increase they will ring up about. They are shown what they pay.</li>
 *   <li><b>No {@code billingCustomerReference}.</b> That is the payment gateway's id for them,
 *       which is ours to hold, not theirs to see.</li>
 *   <li><b>No overrides.</b> "Your limit is 2500" is useful; "your limit was negotiated up from
 *       the plan's 2000" is a commercial conversation, not a billing screen.</li>
 *   <li><b>No {@code planCode}.</b> It is the internal family key. A school reads the name.</li>
 * </ul>
 *
 * <p><b>{@code note} is written for a school to read, not for us.</b> #27's version of it
 * explains that nothing marks a subscription expired yet — true, useful internally, and not
 * something to tell a customer. This one says what it means for them and what to do.
 */
public record MySubscriptionResponse(

        String subscriptionNo,
        SubscriptionStatus status,

        String planName,
        String planDescription,
        Integer planVersion,

        BillingCycle billingCycle,
        BigDecimal price,
        String currencyCode,

        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Long daysRemaining,
        boolean periodEnded,
        Boolean autoRenew,

        Instant cancelledAt,
        String note) {

    public static MySubscriptionResponse fromSubscription(SchoolSubscription subscription,
            PlanDefinition plan) {

        Instant periodEnd = subscription.getCurrentPeriodEnd();
        Instant now = Instant.now();
        boolean ended = periodEnd != null && !periodEnd.isAfter(now);
        Long daysLeft = periodEnd == null ? null : ChronoUnit.DAYS.between(now, periodEnd);

        return new MySubscriptionResponse(
                subscription.getSubscriptionNo(),
                subscription.getStatus(),
                plan.getName(),
                plan.getDescription(),
                subscription.getPlanVersion(),
                subscription.getBillingCycle(),
                // Named `price`, not `contractedPrice`: from where the school sits there is only
                // one price, and "contracted" only means something next to a list price they are
                // not being shown.
                subscription.getContractedPrice(),
                subscription.getCurrencyCode(),
                subscription.getCurrentPeriodStart(),
                periodEnd,
                daysLeft,
                ended,
                subscription.getAutoRenew(),
                subscription.getCancelledAt(),
                noteFor(subscription, ended));
    }

    /**
     * What to tell the school about where its subscription stands.
     *
     * <p>Every branch is written for the person paying. Nothing here mentions which endpoints
     * exist or what the module cannot do yet.
     */
    private static String noteFor(SchoolSubscription subscription, boolean ended) {
        SubscriptionStatus status = subscription.getStatus();
        Instant end = subscription.getCurrentPeriodEnd();

        if (status == SubscriptionStatus.CANCELLED) {
            return "This subscription has been cancelled. Talk to us to start a new one.";
        }
        if (status == SubscriptionStatus.EXPIRED || ended) {
            return "The current period ended on " + end + ". Talk to us to carry on.";
        }
        if (status == SubscriptionStatus.SUSPENDED) {
            return "This subscription is suspended. Talk to us to have it lifted.";
        }
        if (status == SubscriptionStatus.PAST_DUE) {
            return "There is an unpaid invoice on this subscription. Settling it puts the "
                    + "account back in good standing.";
        }
        if (status == SubscriptionStatus.TRIAL) {
            return "This is a trial, running to " + end + ".";
        }
        if (Boolean.FALSE.equals(subscription.getAutoRenew())) {
            return "This subscription does not renew automatically. It ends on " + end + ".";
        }
        return null;
    }
}
