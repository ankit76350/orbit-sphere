package com.orbitastra.backend.models.undone.communication;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.communication.enums.CommunicationChannel;
import com.orbitastra.backend.models.undone.communication.enums.NotificationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Audit log for every communication attempt.
 *
 * A Notification may have multiple CommunicationLogs
 * because delivery can be retried.
 *
 * Examples:
 *
 * Attempt #1
 * SMTP Timeout
 *
 * Attempt #2
 * SMTP Success
 *
 * Attempt #3
 * Open Tracking
 */
@Document(collection = "communication_logs")
@CompoundIndex(name = "notification_attempt_idx", def = "{'notificationDocsId':1,'attemptNumber':1}")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommunicationLog extends SchoolBase {

    // CommunicationCampaign
    // │
    // │ 1
    // ▼
    // Notification
    // │
    // │ 1
    // ▼
    // CommunicationLog

    /**
     * Parent notification.
     */
    @Indexed
    private String notificationDocsId;

    /**
     * Campaign.
     */
    @Indexed
    private String campaignDocsId;

    /**
     * Recipient.
     */
    @Indexed
    private String recipientDocsId;

    /**
     * Delivery channel.
     */
    private CommunicationChannel channel;

    /**
     * Attempt number.
     *
     * First try = 1
     */
    @Builder.Default
    private Integer attemptNumber = 1;

    /**
     * Current status after this attempt.
     */
    private NotificationStatus status;

    /**
     * Provider used.
     *
     * Examples:
     * AWS_SES
     * SMTP
     * FIREBASE
     * APNS
     * TWILIO
     * META_WHATSAPP
     */
    private String provider;

    /**
     * Provider message id.
     */
    private String providerMessageId;

    /**
     * Provider response code.
     *
     * Examples:
     * 200
     * 202
     * 400
     * 500
     */
    private String responseCode;

    /**
     * Provider response message.
     */
    private String responseMessage;

    /**
     * Error code returned by provider.
     */
    private String errorCode;

    /**
     * Error message.
     */
    private String errorMessage;

    /**
     * API endpoint used.
     */
    private String endpoint;

    /**
     * Request payload sent to provider.
     *
     * Useful for debugging.
     */
    private String requestPayload;

    /**
     * Response payload received.
     */
    private String responsePayload;

    /**
     * Delivery latency in milliseconds.
     */
    private Long durationMs;

    /**
     * IP address of webhook/provider callback.
     */
    private String sourceIp;

    /**
     * Event timestamp.
     */
    @Indexed
    @Builder.Default
    private LocalDateTime eventAt = LocalDateTime.now();

}