package com.orbitastra.backend.models.undone.communication.enums;


public enum CommunicationEventType {
    QUEUED,
    SEND_ATTEMPT,
    SENT,
    DELIVERED,
    READ,
    CLICKED,
    BOUNCED,
    FAILED,
    RETRIED,
    WEBHOOK_RECEIVED
}