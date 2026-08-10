package com.orbitastra.backend.models.new_new.audit.enums;

/**
 * Result of an audited operation.
 *
 * <p>Failed and denied attempts are the most security-relevant events, so they
 * are recorded with the same weight as successful ones. An audit trail that only
 * contains successes cannot answer whether anyone tried to reach data they were
 * not entitled to.
 */
public enum AuditOutcome {
    /** The operation completed. */
    SUCCESS,

    /** The operation was permitted but failed, for example a validation error. */
    FAILURE,

    /** The operation was rejected by authentication or authorization. */
    DENIED
}
