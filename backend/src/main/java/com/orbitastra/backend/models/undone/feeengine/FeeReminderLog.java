package com.orbitastra.backend.models.undone.feeengine;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeReminderLog extends BaseDocument {

    @Indexed
    private String studentId;

    // Current rung of the escalation ladder (0-based).
    @Builder.Default
    private Integer stage = 0;

    private ReminderChannel lastChannel;

    private LocalDate lastSentDate;
}
