package com.orbitastra.backend.models.crm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A prospective-student lead captured before a formal admission application is
 * submitted.
 *
 * <p>This document belongs to one school through {@code SchoolBase.schoolId}.
 * It stores prospective guardian and follow-up snapshots as embedded values.
 * A later {@link AdmissionApplication} links back to this document through
 * {@code AdmissionApplication.inquiryDocsId}; the Inquiry deliberately does not
 * store a reverse application-id list.
 *
 * <p>{@code academicYear} stores {@code AcademicYear.name}, never the academic
 * year document id. {@code interestedClassDocsId} references the future final
 * class/grade document.
 *
 * <p><b>A lead is not owned by anybody.</b> It carried an {@code assignedCounselorDocsId} until
 * 2026-09-24, when that field and #11 — the endpoint that would have set it — were removed
 * together. The school works one queue; who logged each interaction is on the follow-up entry,
 * which is a different question and a field that stays.
 */
@Document(collection = "inquiries")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inquiry_no_uniq",
                def = "{'schoolId': 1, 'inquiryNo': 1}",
                unique = true),
        //! assignedCounselorDocsId WAS THE THIRD KEY HERE and was removed on 2026-09-24 with
        //! #11, the endpoint that would have set it. A lead is no longer owned by anybody: the
        //! school works one queue, and #13 narrows it by state and by what is overdue.
        @CompoundIndex(
                name = "school_inquiry_pipeline_idx",
                def = "{'schoolId': 1, 'status': 1, 'nextFollowUpAt': 1}"),
        @CompoundIndex(
                name = "school_inquiry_guardian_phone_idx",
                def = "{'schoolId': 1, 'guardians.phoneNumber': 1}"),
        @CompoundIndex(
                name = "school_inquiry_guardian_email_idx",
                def = "{'schoolId': 1, 'guardians.emailAddress': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry extends SchoolBase {

    // Example: "INQ/2026/09/000001"
    @NotBlank
    private String inquiryNo;

    // Example: "Aarav Sharma"
    @NotBlank
    private String prospectiveStudentName;

    // Example: 2018-08-14
    private LocalDate dateOfBirth;

    // Example: Gender.MALE
    private Gender gender;

    // Example: [{ "fullName": "Rohan Sharma", "relation": "FATHER", "primaryContact": true }]
    @Builder.Default
    private List<InquiryGuardian> guardians = new ArrayList<>();

    // Links to AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to the requested class/grade document id. Example: "67aa15d9dc3f7d0012345678"
    private String interestedClassDocsId;

    // Example: InquiryStatus.NEW
    @NotNull
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    // Example: "WEBSITE"
    private String source;

    // Example: "Google admission campaign"
    private String sourceDetails;

    // Example: 2026-07-15T10:30:00Z
    private Instant nextFollowUpAt;

    // Example: [{ "status": "CONTACTED", "note": "Parent requested fee details" }]
    @Builder.Default
    private List<InquiryFollowUp> followUps = new ArrayList<>();

    // Example: "Parent is interested in transport facilities."
    private String notes;

    // Example: "Relocating to another city"
    private String lostReason;
}
