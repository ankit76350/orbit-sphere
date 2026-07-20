package com.orbitastra.backend.models.undone.feeengine;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.feeengine.enums.ReminderChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks where a fee-defaulter student sits on the reminder escalation ladder,
 * so dunning advances (WhatsApp -> SMS -> Call -> Meeting letter) instead of
 * repeating the same notice.
 */
@Document(collection = "fee_reminder_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeReminderLog {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String studentId;

    // Current rung of the escalation ladder (0-based).
    @Builder.Default
    private Integer stage = 0;

    private ReminderChannel lastChannel;

    private LocalDate lastSentDate;
}
