package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Whether a school's payment gateway is ready to take money.
 */
public enum GatewayConnectionStatus {
    /** Set up has not been done yet. */
    NOT_CONNECTED,

    /** Keys are saved but a test payment has not proved they work. */
    PENDING_VERIFICATION,

    /** Working and able to take payments. */
    CONNECTED,

    /** Blocked for now, either by the school or by the provider. */
    SUSPENDED,

    /** Switched off and no longer in use. */
    DISCONNECTED
}
