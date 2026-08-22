package com.orbitastra.backend.models.crm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A formal application submitted for one {@link AdmissionCycle}.
 *
 * <p>{@code admissionCycleDocsId} is required and references
 * {@code AdmissionCycle.id}. {@code inquiryDocsId} optionally references the
 * originating {@link Inquiry}; it is null for direct applications.
 * {@link AdmissionReview} and {@link AdmissionOffer} documents link to this
 * application through their {@code admissionApplicationDocsId} fields.
 *
 * <p>Applicant and guardian values are submission-time snapshots, so later CRM
 * edits do not rewrite the submitted application. When enrollment succeeds,
 * {@code resultingStudentDocsId} links to the created Student document.
 *
 * <p>Form answers must be checked by the service against
 * {@code applicationFormDefinitionDocsId} and
 * {@code applicationFormVersion}. Offer acceptance, Student creation,
 * application enrollment, and Inquiry closure must be coordinated
 * transactionally by the service layer.
 */
@Document(collection = "admission_applications")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_application_no_uniq",
                def = "{'schoolId': 1, 'applicationNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_cycle_inquiry_uniq",
                def = "{'schoolId': 1, 'admissionCycleDocsId': 1, 'inquiryDocsId': 1}",
                unique = true,
                partialFilter = "{'inquiryDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_cycle_class_status_idx",
                def = "{'schoolId': 1, 'admissionCycleDocsId': 1, 'appliedClassDocsId': 1, 'status': 1, 'submittedAt': 1}"),
        @CompoundIndex(
                name = "school_application_student_uniq",
                def = "{'schoolId': 1, 'resultingStudentDocsId': 1}",
                unique = true,
                partialFilter = "{'resultingStudentDocsId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionApplication extends SchoolBase {

    // Example: "APP/2026/000001"
    @NotBlank
    private String applicationNo;

    // Links to AdmissionCycle.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String admissionCycleDocsId;

    // Optionally links to Inquiry.id. Example: "67aa15d9dc3f7d0022222222"
    private String inquiryDocsId;

    // Links to the requested class/grade document id. Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String appliedClassDocsId;

    // Example: "Aarav Sharma"
    @NotBlank
    private String applicantName;

    // Example: 2018-08-14
    @NotNull
    private LocalDate dateOfBirth;

    // Example: Gender.MALE
    @NotNull
    private Gender gender;

    // Example: [{ "fullName": "Rohan Sharma", "relation": "FATHER", "primaryContact": true }]
    @Builder.Default
    private List<InquiryGuardian> guardians = new ArrayList<>();

    // Example: AdmissionApplicationStatus.DRAFT
    @NotNull
    @Builder.Default
    private AdmissionApplicationStatus status = AdmissionApplicationStatus.DRAFT;

    // Links to the form definition used for this snapshot. Example: "67aa15d9dc3f7d0012121212"
    private String applicationFormDefinitionDocsId;

    // Example: 1
    @NotNull
    @Builder.Default
    private Integer applicationFormVersion = 1;

    // Example: { "previousSchool": "ABC School", "preferredLanguage": "English" }
    @Builder.Default
    private Map<String, Object> formAnswers = new HashMap<>();

    // References DocumentRecord.id values.
    // Example: ["67aa15d9dc3f7d0044444444", "67aa15d9dc3f7d0044444445"]
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Links to the assigned admission staff document. Example: "67aa15d9dc3f7d0055555555"
    private String assignedAdmissionOfficerDocsId;

    // Example: 2026-03-10T09:30:00Z
    private Instant submittedAt;

    // Example: 2026-03-20T12:00:00Z
    private Instant withdrawnAt;

    // Example: "Family relocation was cancelled"
    private String withdrawalReason;

    // Links to the Student created after enrollment. Example: "67aa15d9dc3f7d0066666666"
    private String resultingStudentDocsId;
}
