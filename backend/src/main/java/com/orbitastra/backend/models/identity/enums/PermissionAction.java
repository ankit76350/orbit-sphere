package com.orbitastra.backend.models.identity.enums;

/**
 * One thing a role is allowed to do inside a module.
 *
 * <p>APPROVE is the one that matters most and is deliberately separate from
 * CREATE and EDIT. Several records in this system are raised by one person and
 * decided by another: a concession request, a refund, a staff leave request. That
 * rule cannot be enforced unless "may raise it" and "may allow it" are two
 * different permissions.
 *
 * <p>So a fee desk clerk gets CREATE on concessions and the principal gets
 * APPROVE, and neither of them can do the whole thing alone.
 */
public enum PermissionAction {
    /** May open and read records in this module. */
    VIEW,

    /** May add new records. */
    CREATE,

    /** May change records that already exist. */
    EDIT,

    /** May archive or soft-delete records. */
    DELETE,

    /** May approve or reject what somebody else raised. */
    APPROVE,

    /** May download records as a file or report. */
    EXPORT
}
