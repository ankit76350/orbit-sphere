package com.orbitastra.backend.models.conduct;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.conduct.enums.ConductActionStatus;
import com.orbitastra.backend.models.conduct.enums.ConductActionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One thing the school decided to do about a case, and whether it happened.
 *
 * <p>A case can have several: a written warning and a detention and a parent meeting
 * all from one incident. The old {@code academics/DisciplineLog} held this as a single
 * string called actionTaken, which could say what was decided but never whether it was
 * carried out.
 *
 * <p>That gap is the point of this model. {@code status} with NOT_COMPLETED is how a
 * school notices a detention nobody served. If the only states were pending and done, a
 * skipped action would sit as pending forever and look like a queue rather than a
 * failure, which is exactly how a discipline system quietly stops working.
 *
 * <p>{@code approvedByStaffDocsId} matters most for SUSPENSION and EXPULSION. Those
 * stop a child being educated, and a school will be asked who authorised it. A class
 * teacher deciding a detention needs no approval; a class teacher cannot expel anybody.
 *
 * <p>{@code fineAmount} keeps what the old DisciplineLog had, and connects it properly.
 * A fine is money owed, so it belongs on a bill rather than in a discipline note. The
 * amount is recorded here and {@code feeInvoiceDocsId} is set once finance has billed
 * it under a head with FeeCategory.FINE. Until then the school can see a fine that was
 * decided and never charged.
 *
 * <p>A COUNSELLING_REFERRAL is in this list because it is a real decision a school
 * makes instead of punishing. What the counsellor then records is not here and must not
 * be: those notes need narrower access than the conduct module, the same reason
 * counselling stayed out of health.
 *
 * <p>The service checks that SUSPENSION and EXPULSION carry an approver, that a status
 * of NOT_COMPLETED or CANCELLED carries a reason, that {@code fineAmount} is only set
 * for RESTITUTION, and that an action's student matches its case.
 */
@Document(collection = "conduct_actions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_conduct_action_case_idx",
                def = "{'schoolId': 1, 'studentConductCaseDocsId': 1, 'sequenceNo': 1}"),
        @CompoundIndex(
                name = "school_year_student_action_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'decidedAt': -1}"),
        @CompoundIndex(
                name = "school_conduct_action_pending_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueAt': 1}"),
        @CompoundIndex(
                name = "school_conduct_fine_unbilled_idx",
                def = "{'schoolId': 1, 'actionType': 1, 'feeInvoiceDocsId': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConductAction extends AcademicStudentSchoolBase {

    // Links to StudentConductCase.id. Example: "67b81123dc3f7d0022334455"
    @NotBlank
    private String studentConductCaseDocsId;

    // Order the actions were decided in, starting at 1. Example: 2
    @NotNull
    @Builder.Default
    private Integer sequenceNo = 1;

    // What was decided. Example: ConductActionType.DETENTION
    @NotNull
    private ConductActionType actionType;

    // What it actually involves, in plain words.
    // Example: "Two lunchtime detentions, Tuesday and Wednesday."
    @NotBlank
    private String description;

    // Example: ConductActionStatus.COMPLETED
    @NotNull
    @Builder.Default
    private ConductActionStatus status = ConductActionStatus.PENDING;

    // Links to Staff.id for whoever decided it.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String decidedByStaffDocsId;

    // When it was decided. Example: 2026-08-19T08:00:00Z
    @NotNull
    private Instant decidedAt;

    // Links to Staff.id for whoever authorised it. Required for SUSPENSION and
    // EXPULSION, which stop a child being educated.
    // Example: "67aa15d9dc3f7d0077777777"
    private String approvedByStaffDocsId;

    // When it should be done by. Example: 2026-08-21T12:00:00Z
    private Instant dueAt;

    // When it was done. Null unless the status is COMPLETED.
    // Example: 2026-08-20T07:30:00Z
    private Instant completedAt;

    // Links to Staff.id for whoever saw it through, such as the teacher who
    // supervised the detention. Example: "67aa15d9dc3f7d0044444444"
    private String completedByStaffDocsId;

    // First and last day out of school, for a SUSPENSION. Example: 3
    private Integer suspensionDays;

    // Money the family owes, for a RESTITUTION. Recorded here, billed by finance.
    // Example: 1500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fineAmount;

    // Links to FeeInvoice.id once finance has billed the fine. Null means decided
    // but never charged, which is worth a look.
    // Example: "67ad2233dc3f7d0022334455"
    private String feeInvoiceDocsId;

    // Links to DocumentRecord.id for anything showing it was done, such as a signed
    // detention slip. Example: "67b81124dc3f7d0033445566"
    private String evidenceDocumentDocsId;

    // Why it was not done or was called off. Required for NOT_COMPLETED and
    // CANCELLED. Example: "Child was absent both days; rescheduled to next week."
    private String notCompletedReason;

    // Anything worth knowing.
    // Example: "Father asked to supervise the community service himself."
    private String remarks;
}
