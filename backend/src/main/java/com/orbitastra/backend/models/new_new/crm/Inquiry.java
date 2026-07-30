package com.orbitastra.backend.models.new_new.crm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.new_new.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.new_new.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "inquiries")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inquiry_no_uniq",
                def = "{'schoolId': 1, 'inquiryNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_inquiry_pipeline_idx",
                def = "{'schoolId': 1, 'status': 1, 'assignedCounselorDocsId': 1, 'nextFollowUpAt': 1}"),
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

    // Example: "INQ/2026/000001"
    private String inquiryNo;

    // Example: "Aarav Sharma"
    private String prospectiveStudentName;

    // Example: 2018-08-14
    private LocalDate dateOfBirth;

    // Example: Gender.MALE
    private Gender gender;

    // Example: [{ "fullName": "Rohan Sharma", "relation": "FATHER", "primaryContact": true }]
    @Builder.Default
    private List<InquiryGuardian> guardians = new ArrayList<>();

    // Example: "67aa15d9dc3f7d0098765432"
    private String academicYearDocsId;

    // Example: "67aa15d9dc3f7d0012345678"
    private String interestedClassDocsId;

    // Example: InquiryStatus.NEW
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    // Example: "67aa15d9dc3f7d0055555555"
    private String assignedCounselorDocsId;

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
