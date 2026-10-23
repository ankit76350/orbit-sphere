package com.orbitastra.backend.dto.plans.subscription.request;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * Everything an operator can ask of every school's subscriptions at once. Endpoint #30.
 *
 * <p><b>Every field is optional.</b> A bare {@code GET /platform/subscriptions} is the first page
 * of every subscription on the platform, soonest to end first.
 *
 * <p>The filters combine with <b>AND</b>. Only {@code statuses} and {@code billingCycles} are OR
 * within themselves, because "show me the suspended and the past-due ones" is one question.
 *
 * <h2>This is the same question as #28 asked of everybody</h2>
 *
 * <p>#28 lists one school's subscriptions and takes the school in the URL. This takes no school at
 * all, so the filters here are #28's minus the tenant — deliberately the same names, the same
 * shapes and the same defaults, because an operator who has learned one should not have to learn
 * the other.
 *
 * <p><b>There is no {@code schoolId} filter.</b> Naming one school is what #28 is, and a second
 * way to ask that question would eventually answer it differently.
 *
 * <h2>{@code current} is the one worth understanding</h2>
 *
 * <p>The collection holds <b>one row per billing period</b>, not one per school — a school on its
 * fourth plan has four rows. So a bare list is periods, not schools, and the same school appears
 * several times.
 *
 * <p>{@code ?current=true} is the "one row per school" view, and it is what an operator screen
 * showing "who is on what" actually wants. It is <b>not</b> the default: a list endpoint that
 * quietly filtered would be lying about what it returned, and the count would not match the
 * collection.
 */
public record PlatformSubscriptionSearchRequest(

        /**
         * Repeat the parameter for several. Example: {@code ?status=SUSPENDED&status=PAST_DUE}
         *
         * <p>The headline filter for this endpoint — "who is suspended", "who is past due". Also
         * how you ask for trials, since a trial is a status rather than a field.
         */
        List<SubscriptionStatus> statuses,

        /** Repeat the parameter for several. Example: {@code ?billingCycle=YEARLY} */
        List<BillingCycle> billingCycles,

        /**
         * Exact, case-insensitive, and normalized the same way a code is on the way in — so
         * {@code ?planCode=premium-plus} finds {@code PREMIUM_PLUS}. Example: "PREMIUM"
         *
         * <p>Answers "who is on this plan". Resolved against {@code plan_definitions} first,
         * because a subscription stores the plan's <i>version id</i> and not its code. A code
         * matching no plan gives an empty page rather than an error.
         */
        String planCode,

        /**
         * One version of a plan. Example: 2
         *
         * <p>Stored on the subscription, so it filters on its own — but it is only meaningful
         * beside {@code planCode}, since version 2 of one plan has nothing to do with version 2
         * of another.
         */
        Integer planVersion,

        /** Only auto-renewing subscriptions, or only those that do not. Example: false */
        Boolean autoRenew,

        /**
         * Only the row each school is on now, or only the closed ones. Example: true
         *
         * <p>See the note above: {@code true} is the one-row-per-school view. Not defaulted.
         */
        Boolean current,

        /** Periods starting at or after this instant. Example: 2026-04-01T00:00:00Z */
        Instant startDateFrom,

        /** Periods starting at or before this instant. Example: 2027-03-31T23:59:59Z */
        Instant startDateTo,

        /**
         * Periods ending at or after this instant. Example: 2026-04-01T00:00:00Z
         *
         * <p>This pair is the one an operator reaches for: "everything ending this quarter" is a
         * renewal conversation. #31 is the same question with a days-ahead shorthand.
         */
        Instant endDateFrom,

        /** Periods ending at or before this instant. Example: 2027-03-31T23:59:59Z */
        Instant endDateTo,

        /** Zero-based. Defaults to 0. */
        Integer page,

        /** Defaults to 20, capped at 100. */
        Integer size,

        /**
         * {@code field,direction} — for example {@code currentPeriodEnd,asc}.
         *
         * <p>Sortable on {@code currentPeriodEnd}, {@code currentPeriodStart}, {@code status},
         * {@code contractedPrice}, {@code createdAt} and {@code updatedAt}. An allow-list, so a
         * caller cannot order by a field with no index behind it.
         *
         * <p><b>Not {@code subscriptionNo}</b>, unlike #28. A subscription number is only unique
         * within a school, so across the platform it is neither a meaningful order nor a usable
         * tiebreaker — see the note on the service.
         *
         * <p>Defaults to soonest to end first, which is the order that puts what needs attention
         * at the top.
         */
        String sort) {
}
