package com.orbitastra.backend.models.plans.enums;

/**
 * Current commercial/service state of a SchoolSubscription.
 */
public enum SubscriptionStatus {
    /** Time-limited evaluation; currentPeriodEnd is the trial end boundary. */
    TRIAL,

    /** Subscription is valid and service is available. */
    ACTIVE,

    /**
     * Payment is overdue but final suspension has not occurred.
     *
     * <p><b>A JOB will move a subscription into this, not an endpoint.</b> #18 (mark-past-due)
     * was dropped on 2026-09-07 for that reason: an invoice going unpaid past its terms is the
     * passage of time noticing something, not a decision anybody makes. There is no reason to
     * weigh and nothing to refuse.
     *
     * <p>It still <b>grants</b> everything meanwhile — see
     * {@code SchoolSubscriptionService.whyNotActive}. An unpaid invoice is a conversation, not a
     * reason to lock a school out of its attendance register in the middle of the morning; #19 is
     * what cuts a school off, deliberately and with a reason.
     *
     * <p>An operator who needs it now has #14: {@code {"status": "PAST_DUE", "reason": …}}.
     */
    PAST_DUE,

    /** Subscription access is blocked. */
    SUSPENDED,

    /** Subscription was cancelled and will not renew. */
    CANCELLED,

    /**
     * Contracted period ended without renewal.
     *
     * <p><b>A JOB will move a subscription into this, not an endpoint.</b> #22 (expire) was
     * dropped on 2026-09-08 for the same reason #18 was: its own description said "normally the
     * nightly job does this", and an endpoint whose normal caller is a scheduler is a job with an
     * HTTP door on it. A period end passing is a date arriving, not somebody's decision.
     *
     * <p><b>So nothing sets this today</b>, and that is worth knowing when reading a
     * subscription: a period that has lapsed still shows whatever status it had — usually
     * {@code ACTIVE}, or {@code CANCELLED} after #21 — with {@code periodEnded} true. #27 and #33
     * both report that flag for exactly this reason. Read it alongside the status; the record is
     * never wrong, only untidied.
     *
     * <p>#17 does read it: an {@code EXPIRED} subscription is one of the three that can be
     * renewed, because renewing is precisely what a lapsed period was waiting for.
     */
    EXPIRED
}
