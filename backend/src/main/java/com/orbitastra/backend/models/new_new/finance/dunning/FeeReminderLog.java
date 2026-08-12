package com.orbitastra.backend.models.new_new.finance.dunning;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.ReminderChannel;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Where one family has got to on the fee-reminder ladder.
 *
 * <p>There is one of these per student per academic year, not one per reminder
 * sent. It exists so reminders move forward instead of repeating: a WhatsApp
 * message, then a text, then a call, then a letter asking the parent to come in.
 *
 * <p>{@code stage} is the rung the family is on and {@code lastChannel} is what
 * was last used. Raising the stage is what makes the next reminder stronger than
 * the last one.
 *
 * <p>{@code pausedUntil} matters as much as the rest. A family that has agreed to
 * pay by a certain date, or has an open query, must stop receiving reminders until
 * then, and {@code pauseReason} records why.
 *
 * <p>Only unpaid invoices belong in {@code overdueInvoiceDocsIds}. The service
 * clears the record and drops the stage back once nothing is overdue, so a family
 * that pays up does not stay on the ladder.
 */
@Document(collection = "fee_reminder_logs")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_reminder_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_reminder_due_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'nextReminderDate': 1, 'stage': 1}"),
        @CompoundIndex(
                name = "school_year_reminder_outstanding_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'outstandingAmount': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeReminderLog extends AcademicStudentSchoolBase {

    // Rung of the ladder the family is on, starting at 0. Example: 2
    @NotNull
    @Builder.Default
    private Integer stage = 0;

    // How the last reminder was sent. Example: ReminderChannel.SMS
    private ReminderChannel lastChannel;

    // When the last reminder went out. Example: 2026-05-18T04:30:00Z
    private Instant lastSentAt;

    // When the next reminder is due to go out. Example: 2026-05-25
    private LocalDate nextReminderDate;

    // Reminders sent to this family this year. Example: 3
    @NotNull
    @Builder.Default
    private Integer remindersSent = 0;

    // Money overdue when the last reminder was sent. Example: 11350.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    // Invoices that were overdue at that point. Links to FeeInvoice.id.
    @Builder.Default
    private List<String> overdueInvoiceDocsIds = new ArrayList<>();

    // Links to Guardian.id the reminders are going to.
    // Example: "67aa15d9dc3f7d0066666666"
    private String guardianDocsId;

    // Date the family has promised to pay by. Example: 2026-06-10
    private LocalDate promisedPaymentDate;

    // Reminders are held back until this date. Example: 2026-06-10
    private LocalDate pausedUntil;

    // Why reminders were paused.
    // Example: "Parent has asked for time until salary day and agreed a date."
    private String pauseReason;

    // Links to the staff identity handling this family.
    // Example: "67aa15d9dc3f7d0044444444"
    private String assignedToDocsId;

    // What was said on the last call or at the last meeting.
    // Example: "Father visited on 18 May and agreed to clear it by 10 June."
    private String lastInteractionNote;
}
