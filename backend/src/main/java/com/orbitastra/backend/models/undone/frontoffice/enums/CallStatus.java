package com.orbitastra.backend.models.undone.frontoffice.enums;


public enum CallStatus {

    /**
     * Call logged but no action taken yet.
     */
    OPEN,

    /**
     * Call is being handled or follow-up is in progress.
     */
    IN_PROGRESS,

    /**
     * Call has been completed/resolved.
     */
    CLOSED,

    /**
     * Follow-up required.
     */
    FOLLOW_UP
}