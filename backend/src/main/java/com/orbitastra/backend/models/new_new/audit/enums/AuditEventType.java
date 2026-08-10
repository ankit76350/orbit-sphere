package com.orbitastra.backend.models.new_new.audit.enums;

/**
 * Generic category of an audited operation.
 *
 * <p>This list is deliberately small and stable. The specific business operation
 * is named by the free-text {@code AuditEvent.action}, for example
 * {@code "MARKS_PUBLISHED"} or {@code "REPORT_CARD_REVOKED"}. Keeping domain
 * verbs out of this enum means adding a new workflow never requires editing it.
 */
public enum AuditEventType {
    /** A document was created. */
    CREATE,

    /** One or more fields of an existing document were changed. */
    UPDATE,

    /** A document was soft-deleted or permanently removed. */
    DELETE,

    /** A soft-deleted or archived document was returned to use. */
    RESTORE,

    /** Restricted data was viewed and the access itself must be recorded. */
    READ,

    /** Data left the system as a download, report, or integration extract. */
    EXPORT,

    /** An authentication attempt. */
    LOGIN,

    /** A session was ended. */
    LOGOUT,

    /** A role, permission, or access assignment changed. */
    PERMISSION_CHANGE,

    /** A workflow state changed, such as publish, lock, approve, or cancel. */
    STATE_CHANGE,

    /** Category not represented above; {@code action} carries the detail. */
    OTHER
}
