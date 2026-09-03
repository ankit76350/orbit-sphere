package com.orbitastra.backend.models.plans.enums;

/**
 * What a {@code usageLimit} counts.
 *
 * <p>A limit is a bare number — 2000 — and on its own it cannot be enforced: nothing downstream
 * would know whether it meant students, staff, vehicles or megabytes. This says which counter the
 * entitlement service reads.
 *
 * <p><b>Callers never choose one.</b> Each {@link FeatureCode} declares the metric it is measured
 * in, because a feature knows what it counts: students are counted in students. That makes
 * "student management, limited to 2000 gigabytes" unrepresentable rather than merely refused.
 *
 * <p>Every constant here needs a counter somebody actually implements. Adding one that nothing
 * measures produces a limit that is never reached and never enforced, which is worse than having
 * no limit at all — the plan reads as capped and behaves as unlimited.
 */
public enum UsageMetric {

    /** Students with an active academic record in the current year. */
    ACTIVE_STUDENTS,

    /** Staff members employed and not offboarded. */
    ACTIVE_STAFF,

    /** Login accounts across every role. */
    USER_ACCOUNTS,

    /** Vehicles on the school's transport fleet. */
    VEHICLES,

    /** Beds across every hostel block. */
    HOSTEL_BEDS,

    /** Distinct titles in the library catalogue. */
    LIBRARY_TITLES,

    /** Stored files and attachments, in megabytes. */
    STORAGE_MEGABYTES,

    /** Text messages sent in a billing period. */
    SMS_MESSAGES,

    /** Emails sent in a billing period. */
    EMAIL_MESSAGES
}
