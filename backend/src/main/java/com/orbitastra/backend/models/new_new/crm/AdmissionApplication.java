package com.orbitastra.backend.models.new_new.crm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    private String applicationNo;

    // Example: "67aa15d9dc3f7d0011111111"
    private String admissionCycleDocsId;

    // Example: "67aa15d9dc3f7d0022222222"
    private String inquiryDocsId;

    // Example: "67aa15d9dc3f7d0033333333"
    private String appliedClassDocsId;

    // Example: "Aarav Sharma"
    private String applicantName;

    // Example: 2018-08-14
    private LocalDate dateOfBirth;

    // Example: Gender.MALE
    private Gender gender;

    // Example: [{ "fullName": "Rohan Sharma", "relation": "FATHER", "primaryContact": true }]
    @Builder.Default
    private List<InquiryGuardian> guardians = new ArrayList<>();

    // Example: AdmissionApplicationStatus.DRAFT
    @Builder.Default
    private AdmissionApplicationStatus status = AdmissionApplicationStatus.DRAFT;

    // Example: 1
    @Builder.Default
    private Integer formVersion = 1;

    // Example: { "previousSchool": "ABC School", "preferredLanguage": "English" }
    @Builder.Default
    private Map<String, Object> formAnswers = new HashMap<>();

    // Example: ["67aa15d9dc3f7d0044444444", "67aa15d9dc3f7d0044444445"]
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Example: "67aa15d9dc3f7d0055555555"
    private String assignedAdmissionOfficerDocsId;

    // Example: 2026-03-10T09:30:00Z
    private Instant submittedAt;

    // Example: 2026-03-20T12:00:00Z
    private Instant withdrawnAt;

    // Example: "Family relocation was cancelled"
    private String withdrawalReason;

    // Example: "67aa15d9dc3f7d0066666666"
    private String resultingStudentDocsId;
}
