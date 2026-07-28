package com.orbitastra.backend.models.undone.frontoffice.enums;


public enum PostalStatus {

    /**
     * Received by the school.
     */
    RECEIVED,

    /**
     * Ready to be dispatched.
     */
    PENDING,

    /**
     * Dispatched from the school.
     */
    DISPATCHED,

    /**
     * Successfully delivered.
     */
    DELIVERED,

    /**
     * Returned to the sender.
     */
    RETURNED,

    /**
     * Delivery failed or lost.
     */
    FAILED
}
