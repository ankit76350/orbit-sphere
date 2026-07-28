package com.orbitastra.backend.models.undone.communication;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.communication.enums.AudienceType;
import com.orbitastra.backend.models.undone.communication.enums.CampaignStatus;
import com.orbitastra.backend.models.undone.communication.enums.CommunicationChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents a communication campaign.
 *
 * One campaign may send messages through one or more channels
 * (Email, SMS, Push, WhatsApp, In-App).
 *
 * Example:
 *
 * Fee Reminder
 * ↓
 * Parents of Grade 10
 * ↓
 * EMAIL + SMS + PUSH
 *
 * One campaign creates many Notification documents
 * (one per recipient).
 */
@Document(collection = "communication_campaigns")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommunicationCampaign extends SchoolBase {

    // CommunicationCampaign
    //     │
    //     │ 1
    //     │
    //     ├──────────────► MessageTemplate
    //     │
    //     │
    //     └──────────────► Notification (1:N)

    /**
     * Campaign title.
     *
     * Example:
     * Fee Reminder
     * Holiday Notice
     */
    @Indexed
    private String title;

    /**
     * Subject.
     *
     * Used mainly for Email.
     */
    private String subject;

    /**
     * Message body.
     *
     * Can contain template variables.
     */
    private String body;

    /**
     * Template used.
     *
     * Optional.
     */
    private String templateDocsId;

    /**
     * Delivery channels.
     *
     * Example:
     * EMAIL
     * SMS
     * PUSH
     */
    @Builder.Default
    private List<CommunicationChannel> channels = new ArrayList<>();

    /**
     * Audience type.
     */
    private AudienceType audienceType;

    /**
     * Audience values.
     *
     * Examples:
     * Grade 10
     * Section A
     * Student DocsIds
     */
    @Builder.Default
    private List<String> audienceValues = new ArrayList<>();

    /**
     * Optional attachment URLs.
     */
    @Builder.Default
    private List<String> attachmentUrls = new ArrayList<>();

    /**
     * Current campaign status.
     */
    @Indexed
    private CampaignStatus status;

    /**
     * Schedule for future delivery.
     *
     * Null = Send Immediately.
     */
    @Indexed
    private LocalDateTime scheduledAt;

    /**
     * Actual sending started.
     */
    private LocalDateTime startedAt;

    /**
     * Campaign finished.
     */
    private LocalDateTime completedAt;

    /**
     * Staff/Admin who created this campaign.
     */
    private String createdByDocsId;

    /**
     * Total recipients.
     */
    @Builder.Default
    private Integer totalRecipients = 0;

    /**
     * Successfully sent.
     */
    @Builder.Default
    private Integer sentCount = 0;

    /**
     * Successfully delivered.
     */
    @Builder.Default
    private Integer deliveredCount = 0;

    /**
     * Read by recipient.
     */
    @Builder.Default
    private Integer readCount = 0;

    /**
     * Failed deliveries.
     */
    @Builder.Default
    private Integer failedCount = 0;

    /**
     * Clicked notifications.
     */
    @Builder.Default
    private Integer clickedCount = 0;

    /**
     * Additional notes.
     */
    private String remarks;

}
