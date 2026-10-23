package com.orbitastra.backend.models.identity.enums;

/**
 * Whether a login can be used right now.
 *
 * <p>LOCKED and SUSPENDED look the same to the person trying to log in, but they
 * are not the same thing. LOCKED is the system protecting itself after too many
 * wrong passwords, and it clears on its own. SUSPENDED is a person deciding this
 * account should stop working, and only a person can undo it.
 */
public enum UserAccountStatus {
    /** Created and sent to the person, but they have not set a password yet. */
    INVITED,

    /** Working normally. */
    ACTIVE,

    /** Turned off by an administrator, and only an administrator can turn it back on. */
    SUSPENDED,

    /** Held shut after too many wrong passwords. Opens again by itself. */
    LOCKED,

    /** Closed for good, usually because the person has left the school. */
    DISABLED
}
