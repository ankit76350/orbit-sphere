package com.orbitastra.backend.models.undone.communication;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;
import com.orbitastra.backend.models.undone.communication.enums.CommChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A multi-channel broadcast campaign (WhatsApp/SMS/Email/Push) to an audience,
 * with delivery/read analytics. Distinct from {@code core.Notification} (a single
 * per-recipient message) and {@code core.Announcement} (a static bulletin).
 */
@Document(collection = "comm_broadcasts")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommBroadcast extends SchoolDocs {

    private CommChannel channel;

    // Target audience label, e.g. "Whole School", "Grade 10", "Fee Defaulters".
    private String audience;

    private String preview;

    // Delivery analytics.
    @Builder.Default
    private Integer sentCount = 0;
    @Builder.Default
    private Integer deliveredCount = 0;
    @Builder.Default
    private Integer readCount = 0;

    // Message credits consumed by the send.
    private Integer credits;

    private String templateId; // references MessageTemplate.id

    private String language;

    private String sentBy; // references Staff.id / User.id
}
