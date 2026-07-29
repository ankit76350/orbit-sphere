package com.orbitastra.backend.models.undone.a_latter.transport.enums;


public enum AllocationStatus {

    /**
     * Student is actively using transport.
     */
    ACTIVE,

    /**
     * Allocation temporarily suspended.
     */
    SUSPENDED,

    /**
     * Allocation completed.
     *
     * Example:
     * Student passed out.
     */
    COMPLETED,

    /**
     * Allocation cancelled.
     */
    CANCELLED

}
