package com.orbitastra.backend.dto.plans.subscription.response;

import java.time.Instant;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SubscriptionHistory;
import com.orbitastra.backend.models.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * One thing that happened to a subscription. Endpoint #29.
 *
 * <p><b>What changed, when it changed, who changed it and why</b> — one row of the trail, in the
 * order a person reads those four questions. Nothing here is computed: every field is either
 * stored on the history row or is the plan that row's stored id points at.
 *
 * <h2>Nothing is inferred, and a gap is left as a gap</h2>
 *
 * <p>An audit trail is read to settle what actually happened, so a field this DTO cannot answer
 * is <b>null</b> rather than filled in with something reasonable. In particular:
 *
 * <ul>
 * <li>{@code previousStatus} is null on a subscription's first row, because there was no previous
 *     status. It does not mean "unknown".</li>
 * <li>{@code performedByDocsId} is null on <b>every row that exists today</b> — nothing populates
 *     it yet, since #13 does not resolve the acting account. {@code source} is the only answer to
 *     "who" the record currently holds, and {@code ADMIN_PORTAL} is the only value written.</li>
 * <li>{@code reason} is null whenever the caller who made the change did not give one.</li>
 * <li>A plan whose document has since been deleted leaves that side's three plan fields null,
 *     rather than failing the page. A history is exactly where a deleted plan turns up.</li>
 * </ul>
 *
 * <h2>Both plans are named, not just linked</h2>
 *
 * <p>A {@code PLAN_CHANGED} row stores two ids, and a pair of Mongo ids cannot answer the
 * question the row exists for — nobody can tell from them whether a school moved from Premium v1
 * to Standard v3. So each side carries its code, version and name.
 *
 * <p>The plans behind a page are fetched in <b>one</b> query and handed in, so a trail of twenty
 * plan changes costs one plan read rather than forty. See the note on the service.
 *
 * <h2>The two dates are both here, and they are different</h2>
 *
 * <p>{@code effectiveAt} is when the change took effect; {@code recordedAt} is when the row was
 * written. A cancellation agreed today for the end of the period is effective at the end of the
 * period and recorded today. Returning only one would make the trail look wrong to whoever read
 * it against an invoice.
 *
 * <p><b>{@code schoolSubscriptionDocsId} is deliberately absent.</b> The caller named the
 * subscription in the URL, so echoing its internal id back on all twenty rows tells them nothing
 * they did not just send. {@code subscriptionNo} is here instead — the number printed on the
 * record — so a row copied out of a page still says what it belongs to.
 */
public record SubscriptionHistoryEntryResponse(
        String historyId,
        String subscriptionNo,
        SubscriptionEventType eventType,
        SubscriptionStatus previousStatus,
        SubscriptionStatus newStatus,
        String previousPlanCode,
        Integer previousPlanVersion,
        String previousPlanName,
        String newPlanCode,
        Integer newPlanVersion,
        String newPlanName,
        String reason,
        String source,
        String sourceEventId,
        String performedByDocsId,
        Instant effectiveAt,
        Instant recordedAt) {

    /**
     * @param subscriptionNo the number of the subscription this row belongs to. Passed in rather
     *                       than read off the row, because a history row stores the
     *                       subscription's id and not its number — and the caller already named
     *                       the subscription, so it is one value for the whole page rather than
     *                       one lookup per row.
     * @param previousPlan   the plan moved off, or {@code null} when the row records no plan move
     *                       or that plan document has gone
     * @param newPlan        the plan moved to, on the same terms
     */
    public static SubscriptionHistoryEntryResponse fromHistory(SubscriptionHistory entry,
            String subscriptionNo, PlanDefinition previousPlan, PlanDefinition newPlan) {

        return new SubscriptionHistoryEntryResponse(
                entry.getId(),
                subscriptionNo,
                entry.getEventType(),
                entry.getPreviousStatus(),
                entry.getNewStatus(),
                previousPlan == null ? null : previousPlan.getPlanCode(),
                previousPlan == null ? null : previousPlan.getPlanVersion(),
                previousPlan == null ? null : previousPlan.getName(),
                newPlan == null ? null : newPlan.getPlanCode(),
                newPlan == null ? null : newPlan.getPlanVersion(),
                newPlan == null ? null : newPlan.getName(),
                entry.getReason(),
                entry.getSource(),
                entry.getSourceEventId(),
                entry.getPerformedByDocsId(),
                entry.getEffectiveAt(),
                entry.getCreatedAt());
    }
}
