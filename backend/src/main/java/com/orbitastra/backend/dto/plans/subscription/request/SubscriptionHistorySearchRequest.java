package com.orbitastra.backend.dto.plans.subscription.request;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * Everything a caller can ask of one subscription's audit trail. Endpoint #29.
 *
 * <p><b>Every field is optional.</b> A bare
 * {@code GET /platform/schools/{id}/subscriptions/{no}/history} is the first page of the whole
 * trail, newest change first, which is what somebody opening a history wants to see.
 *
 * <p>The filters combine with <b>AND</b>. Only {@code eventTypes}, {@code newStatuses} and
 * {@code previousStatuses} are OR within themselves, because "show me the suspensions and the
 * cancellations" is one question.
 *
 * <h2>Every filter here is a field that exists on the document</h2>
 *
 * <p>Nothing is derived and nothing is inferred, which matters more on an audit trail than
 * anywhere else: a filter computed from other fields would quietly decide what counts as a
 * suspension, and the answer to "why was this school cut off" must not depend on that.
 *
 * <p>So there is <b>no plan-code filter</b>, unlike #28. A history row stores
 * {@code previousPlanDefinitionDocsId} and {@code newPlanDefinitionDocsId} — two plan links, not
 * one — and "rows involving PREMIUM" would have to guess whether the caller means moved-off or
 * moved-to. {@code ?eventType=PLAN_CHANGED} is the question that actually gets asked, and the
 * response names both plans so the reader can see which. Say if the two-sided filter is wanted.
 *
 * <h2>Two date windows, because a history row has two dates</h2>
 *
 * <p>{@code effectiveAt} is when the change took effect and {@code createdAt} is when the row was
 * written, and they are <b>not the same</b>: a cancellation agreed today for the end of the
 * period is dated at the end of the period. Filtering on the wrong one silently answers a
 * different question, so both are offered and each is named after what it means.
 */
public record SubscriptionHistorySearchRequest(

        /**
         * What happened. Repeat the parameter for several.
         * Example: {@code ?eventType=SUSPENDED&eventType=RESUMED}
         *
         * <p>This is the "action" filter. Every value of the enum is written by something — see
         * the field table in the module README.
         */
        List<SubscriptionEventType> eventTypes,

        /**
         * The status the change moved the subscription <b>to</b>. Repeat for several.
         * Example: {@code ?status=SUSPENDED}
         *
         * <p>This is the one most questions mean: "when was it suspended" is a row whose
         * {@code newStatus} is {@code SUSPENDED}.
         */
        List<SubscriptionStatus> newStatuses,

        /**
         * The status it moved <b>from</b>. Repeat for several.
         * Example: {@code ?previousStatus=TRIAL}
         *
         * <p>Separate from {@code newStatuses} because a transition has two ends and they answer
         * different questions — {@code ?previousStatus=TRIAL} is "when did the trial end", which
         * no filter on the new status can express.
         *
         * <p>Matches nothing on the first row of a subscription, which has no previous status.
         */
        List<SubscriptionStatus> previousStatuses,

        /**
         * Where the change came from. Exact, case-insensitive. Example: "ADMIN_PORTAL"
         *
         * <p>{@code ADMIN_PORTAL} is the only value written today. It is a {@code String} on the
         * document rather than an enum, so this cannot be a typed parameter without rejecting
         * values that are legitimately stored.
         */
        String source,

        /**
         * The identity that acted. Exact match on the stored id. Example: "67aa2b73dc3f7d0099887766"
         *
         * <p>This is the "changedBy" filter. <b>It matches nothing today</b>, because nothing
         * populates {@code performedByDocsId} yet — #13 does not resolve the acting account, so
         * every existing row has it null. The filter is here because the field is, and because a
         * trail that could not be narrowed to one operator once it is populated would need this
         * adding later anyway.
         */
        String performedByDocsId,

        /**
         * The external event that caused the change, if anything outside our system did.
         * Example: "billing_event_00004519"
         *
         * <p>Null on everything a person did, which is every row today. It is what traces a row
         * back to a payment-gateway webhook or a job run.
         */
        String sourceEventId,

        /**
         * Free-text search of the reason. Case-insensitive, matches anywhere in it.
         * Example: {@code ?reason=non-payment}
         *
         * <p>A substring rather than an exact match, because {@code reason} is whatever the
         * operator typed and nobody can reproduce it exactly months later. The text is escaped
         * before it reaches the query, so a reason containing {@code .*} searches for those two
         * characters rather than matching every row.
         */
        String reason,

        /** Changes that took effect at or after this instant. Example: 2026-04-01T00:00:00Z */
        Instant effectiveFrom,

        /** Changes that took effect at or before this instant. Example: 2027-03-31T23:59:59Z */
        Instant effectiveTo,

        /** Rows written at or after this instant. Example: 2026-04-01T00:00:00Z */
        Instant recordedFrom,

        /** Rows written at or before this instant. Example: 2027-03-31T23:59:59Z */
        Instant recordedTo,

        /** Zero-based. Defaults to 0. */
        Integer page,

        /** Defaults to 20, capped at 100. */
        Integer size,

        /**
         * {@code field,direction} — for example {@code effectiveAt,asc}.
         *
         * <p>Sortable on {@code effectiveAt}, {@code createdAt}, {@code eventType},
         * {@code newStatus} and {@code previousStatus}. An allow-list, so a caller cannot order
         * by a field the endpoint does not mean to expose or probe the document's shape by
         * guessing names.
         *
         * <p>Defaults to newest change first. Whatever is asked for, the default order follows
         * it as a tiebreaker so paging is stable — see the note on the service.
         */
        String sort) {
}
