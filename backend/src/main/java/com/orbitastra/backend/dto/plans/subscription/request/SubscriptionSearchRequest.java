package com.orbitastra.backend.dto.plans.subscription.request;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * Everything a caller can ask of one school's subscription history. Endpoint #28.
 *
 * <p><b>Every field is optional.</b> A bare {@code GET /platform/schools/{id}/subscriptions} is
 * the first page of every subscription that school has ever had — live, lapsed, cancelled and
 * superseded alike. That is the point of the endpoint: #27 answers "what are they on now", and
 * this one answers "what have they been on".
 *
 * <p>The filters combine with <b>AND</b>. Only {@code statuses} and {@code billingCycles} are OR
 * within themselves, because "show me the cancelled and the expired ones" is one question.
 *
 * <h2>Why there is no `trial` filter</h2>
 *
 * <p>There is no {@code trial} field on {@code school_subscriptions}. A trial is a
 * <b>status</b> — {@code TRIAL} — so {@code ?status=TRIAL} is the whole answer, and a separate
 * boolean would be a second way to ask the same question that could disagree with the first.
 * #13 takes a {@code trial} flag on the way in and it lands on {@code status}; nothing keeps it
 * as a field of its own.
 *
 * <h2>Why planCode is not a field on the document either</h2>
 *
 * <p>A subscription links to a plan <i>version</i> by {@code planDefinitionDocsId}. The code
 * lives on {@code plan_definitions}, so filtering by it means resolving the code to the version
 * ids first — one extra query, still done in the database, never by loading subscriptions and
 * sifting them. A code that matches no plan gives an <b>empty page</b> rather than an error: a
 * filter that matches nothing is a legitimate answer, and #8 treats an unmatched filter the same
 * way.
 */
public record SubscriptionSearchRequest(

        /**
         * Repeat the parameter for several. Example: {@code ?status=CANCELLED&status=EXPIRED}
         *
         * <p>This is also how you ask for trials — {@code ?status=TRIAL}. See the note above.
         */
        List<SubscriptionStatus> statuses,

        /** Repeat the parameter for several. Example: {@code ?billingCycle=MONTHLY} */
        List<BillingCycle> billingCycles,

        /**
         * Exact, case-insensitive, and normalized the same way a code is on the way in — so
         * {@code ?planCode=premium-plus} finds {@code PREMIUM_PLUS}. Example: "PREMIUM"
         *
         * <p>Every version of that plan unless {@code planVersion} narrows it. Resolved against
         * {@code plan_definitions} first; see the note above.
         */
        String planCode,

        /**
         * One version of a plan. Example: 2
         *
         * <p>Stored on the subscription, so this filters on its own — but it is only meaningful
         * beside {@code planCode}, since version 2 of one plan has nothing to do with version 2
         * of another.
         */
        Integer planVersion,

        /** Only auto-renewing subscriptions, or only those that do not. Example: true */
        Boolean autoRenew,

        /**
         * Only the row a school is on now, or only the closed ones. Example: false
         *
         * <p>{@code current=true} is at most one row and is what #27 returns; {@code false} is
         * the history without it.
         */
        Boolean current,

        /** Periods starting at or after this instant. Example: 2026-04-01T00:00:00Z */
        Instant startDateFrom,

        /** Periods starting at or before this instant. Example: 2027-03-31T23:59:59Z */
        Instant startDateTo,

        /** Periods ending at or after this instant. Example: 2026-04-01T00:00:00Z */
        Instant endDateFrom,

        /** Periods ending at or before this instant. Example: 2027-03-31T23:59:59Z */
        Instant endDateTo,

        /** Zero-based. Defaults to 0. */
        Integer page,

        /** Defaults to 20, capped at 100. */
        Integer size,

        /**
         * {@code field,direction} — for example {@code currentPeriodStart,desc}.
         *
         * <p>Sortable on {@code currentPeriodStart}, {@code currentPeriodEnd},
         * {@code subscriptionNo}, {@code status}, {@code createdAt} and {@code updatedAt}. An
         * allow-list, so a caller cannot order by a field with no index behind it.
         */
        String sort) {
}
