package com.orbitastra.backend.models.new_new.identity.enums;

/**
 * Why a login session stopped working.
 *
 * <p>Kept so somebody asking "why was I signed out?" can be given a real answer,
 * and so an administrator can see whether a session was ended normally or taken
 * away.
 */
public enum SessionEndReason {
    /** The person signed out themself. */
    SIGNED_OUT,

    /** The session ran past its end time. */
    EXPIRED,

    /** An administrator ended it. */
    ENDED_BY_ADMIN,

    /** Ended because the password was changed, which signs out every device. */
    PASSWORD_CHANGED,

    /** Ended because the account was suspended or closed. */
    ACCOUNT_CLOSED
}
