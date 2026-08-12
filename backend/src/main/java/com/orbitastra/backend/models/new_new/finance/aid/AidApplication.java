package com.orbitastra.backend.models.new_new.finance.aid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.AidApplicationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A family asking for help with fees under one AidProgramme.
 *
 * <p>Household income and similar details are private. They are kept in
 * {@code encryptedHouseholdAssessment} rather than as plain fields, so a fee-desk
 * user working on invoices never sees a family's finances in a list or an export.
 *
 * <p>Checking the paperwork and deciding the outcome are separate steps with
 * separate people: {@code verifiedByDocsId} confirms the documents are genuine,
 * and {@code decidedByDocsId} grants or refuses the help.
 *
 * <p>One student may only apply once per programme per year, which the unique
 * index enforces. An approved application should have exactly one AidAward
 * pointing back at it.
 */
@Document(collection = "aid_applications")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_aid_application_no_uniq",
                def = "{'schoolId': 1, 'applicationNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_programme_student_aid_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'aidProgrammeDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_aid_status_submitted_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'submittedAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AidApplication extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type AID_APPLICATION.
    // Example: "AID/2026/000042"
    @NotBlank
    private String applicationNo;

    // Links to AidProgramme.id. Example: "67b02233dc3f7d0022334455"
    @NotBlank
    private String aidProgrammeDocsId;

    // Links to Guardian.id for the parent who applied.
    // Example: "67aa15d9dc3f7d0066666666"
    private String guardianDocsId;

    // Example: AidApplicationStatus.APPROVED
    @NotNull
    @Builder.Default
    private AidApplicationStatus status = AidApplicationStatus.DRAFT;

    // Help the family asked for. Example: 40000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal requestedAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Private household and income details, encrypted before being saved.
    // Example: "enc:v1:7c6b5a4938271605f4e3d2c1"
    private String encryptedHouseholdAssessment;

    // Example: 2026-03-18T07:05:00Z
    private Instant submittedAt;

    // Proof the family sent. Links to DocumentRecord.id.
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // What the checker found, written for the committee to read.
    // Example: "Income certificate and mark sheet both verified as genuine."
    private String verificationSummary;

    // Links to the staff identity that checked the documents.
    // Example: "67aa15d9dc3f7d0044444444"
    private String verifiedByDocsId;

    // Example: 2026-03-22T09:30:00Z
    private Instant verifiedAt;

    // Score the committee gave, used to rank applications. Example: 87
    private Integer reviewScore;

    // Note left by the committee.
    // Example: "Ranked second among class VIII applicants."
    private String reviewRemarks;

    // Links to the staff identity that granted or refused the help. Must be a
    // different person from the one who verified it, which the service checks.
    // Example: "67aa15d9dc3f7d0055555555"
    private String decidedByDocsId;

    // Example: 2026-03-28T10:15:00Z
    private Instant decidedAt;

    // Why the application was refused, and what the family was told.
    // Example: "Household income is above the limit set for this programme."
    private String decisionReason;
}
