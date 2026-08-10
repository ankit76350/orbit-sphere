package com.orbitastra.backend.models.new_new.audit.enums;

/**
 * Kind of principal that performed an audited operation.
 *
 * <p>This distinguishes a human action from an automated one, which a single
 * actor id cannot express. {@code AuditEvent.actorDocsId} is null for
 * {@code SYSTEM}.
 */
public enum AuditActorType {
    /** A staff member acting through the application. */
    STAFF,

    /** A student acting through a student portal. */
    STUDENT,

    /** A guardian acting through a guardian portal. */
    GUARDIAN,

    /** A scheduled job, migration, or internal background process. */
    SYSTEM,

    /** An external system acting through an integration or API credential. */
    INTEGRATION,

    /** Platform support staff acting inside a school tenant. */
    PLATFORM_SUPPORT
}
