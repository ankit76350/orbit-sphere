package com.orbitastra.backend.models.audit.enums;

/**
 * The broad kind of thing that happened.
 *
 * <p>Deliberately a short, stable list. The specific operation lives in
 * {@code AuditEvent.action} as text, so a new workflow never needs an enum change. This is
 * only for grouping and for deciding how long something has to be kept.
 *
 * <p>READ is the one people leave out and then wish they had. Somebody opening a child's
 * medical record or a colleague's payslip is often the only thing worth auditing at all,
 * because nothing changed and no other trace exists. It is not recorded for everything —
 * logging every list screen would bury the collection — but it must be recorded for the
 * places that matter.
 *
 * <p>ACCESS_DENIED is separate from a failure. A refusal is somebody trying to do something
 * they may not, and a run of them in one afternoon is the clearest signal this system can
 * give that something is wrong.
 */
public enum AuditEventType {
    /** A new record was made. */
    CREATE,

    /** An existing record was changed. */
    UPDATE,

    /** A record was archived or soft-deleted. */
    DELETE,

    /** Something sensitive was looked at. Recorded selectively, never for everything. */
    READ,

    /** Somebody signed in, signed out, or failed to. */
    AUTHENTICATION,

    /** Roles or permissions were granted or taken away. */
    AUTHORIZATION_CHANGE,

    /** Somebody was refused. Worth watching in runs rather than one at a time. */
    ACCESS_DENIED,

    /** A record moved from one state to another, such as a payroll run being approved. */
    STATE_CHANGE,

    /** Money moved, or a figure that becomes money was set. */
    FINANCIAL,

    /** Data left the school: a report, an export, a file download. */
    DATA_EXPORT,

    /** A consent was granted or withdrawn. */
    CONSENT_CHANGE,

    /** A setting that changes how the school behaves was altered. */
    CONFIGURATION_CHANGE,

    /** The system did it, with no person involved: a nightly job, a webhook. */
    SYSTEM_ACTION
}
