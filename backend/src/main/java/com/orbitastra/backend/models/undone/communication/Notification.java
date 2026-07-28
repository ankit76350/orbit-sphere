package com.orbitastra.backend.models.undone.communication;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.communication.enums.CommunicationChannel;
import com.orbitastra.backend.models.undone.communication.enums.NotificationStatus;
import com.orbitastra.backend.models.undone.communication.enums.RecipientType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents a single notification delivered to a single recipient.
 *
 * Examples:
 *
 * Campaign
 * --------
 * Fee Reminder
 *
 * Recipients
 * ----------
 * Parent A
 * Parent B
 * Parent C
 *
 * Result
 * ------
 * Notification A
 * Notification B
 * Notification C
 */
@Document(collection = "notifications")
@CompoundIndex(
        name = "recipient_status_idx",
        def = "{'recipientDocsId':1,'status':1}"
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends SchoolBase {

    // CommunicationCampaign
    //         │
    //         │ 1
    //         ▼
    // Notification
    //         │
    //         │ 1
    //         ▼
    // CommunicationLog

    /**
     * Communication campaign that generated this notification.
     */
    @Indexed
    private String campaignDocsId;

    /**
     * Recipient User/Student/Parent/Staff docsId.
     */
    @Indexed
    private String recipientDocsId;

    /**
     * Recipient type.
     */
    private RecipientType recipientType;

    /**
     * Delivery channel.
     */
    private CommunicationChannel channel;

    /**
     * Notification title.
     */
    private String title;

    /**
     * Notification body.
     */
    private String body;

    /**
     * Optional image.
     */
    private String imageUrl;

    /**
     * Optional icon.
     */
    private String iconUrl;

    /**
     * Optional attachment.
     */
    private String attachmentUrl;

    /**
     * Deep link inside mobile/web application.
     *
     * Example:
     * /fees/12345
     * /attendance
     */
    private String actionUrl;

    /**
     * Current notification status.
     */
    @Indexed
    private NotificationStatus status;

    /**
     * Number of delivery attempts.
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Scheduled delivery time.
     */
    @Indexed
    private LocalDateTime scheduledAt;

    /**
     * Sent timestamp.
     */
    private LocalDateTime sentAt;

    /**
     * Delivered timestamp.
     */
    private LocalDateTime deliveredAt;

    /**
     * Read timestamp.
     */
    private LocalDateTime readAt;

    /**
     * Clicked timestamp.
     */
    private LocalDateTime clickedAt;

    /**
     * Failed timestamp.
     */
    private LocalDateTime failedAt;

    /**
     * Failure reason.
     */
    private String failureReason;

    /**
     * Provider message id.
     *
     * Examples:
     * Firebase Message Id
     * SES Message Id
     * Twilio SID
     */
    private String providerMessageId;

    /**
     * Additional metadata.
     *
     * Examples:
     * feeId
     * studentId
     * invoiceId
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Soft delete flag.
     */
    @Builder.Default
    private Boolean deleted = false;

}