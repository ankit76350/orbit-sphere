package com.orbitastra.backend.models.new_new.audit.enums;

/**
 * What kind of principal did it.
 *
 * <p>Says which collection {@code AuditEvent.actorDocsId} points at, so one trail covers
 * everybody rather than needing a log per kind of user.
 *
 * <p>SYSTEM matters more than it looks. A nightly job moving overdue books, a payment gateway
 * webhook, a scheduled payroll computation — those all change records with no person behind
 * them. Without a value for it, either the actor is left null and the row looks broken, or
 * somebody invents a fake staff account and the trail starts lying.
 */
public enum AuditActorType {
    /** A member of staff. Points at Staff.id. */
    STAFF,

    /** A parent or guardian, usually from the parent portal. Points at Guardian.id. */
    GUARDIAN,

    /** A student. Points at Student.id. */
    STUDENT,

    /** A scheduled job or an internal process. No person, so actorDocsId is null. */
    SYSTEM,

    /** An outside system calling in, such as a payment gateway webhook. */
    INTEGRATION,

    /** Somebody who was not signed in, on a public page such as a certificate check. */
    ANONYMOUS
}
