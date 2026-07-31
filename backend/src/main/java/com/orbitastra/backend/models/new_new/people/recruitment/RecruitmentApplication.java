package com.orbitastra.backend.models.new_new.people.recruitment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.recruitment.embedded.RecruitmentInterview;
import com.orbitastra.backend.models.new_new.people.recruitment.enums.RecruitmentStage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Candidate application submitted for one JobVacancy.
 *
 * <p>Candidate PII is kept in an encrypted profile. A keyed lookup hash allows
 * exact duplicate detection within the vacancy without exposing email or phone
 * values. When hiring completes, {@code resultingStaffDocsId} links to the
 * created Staff profile.
 */
@Document(collection = "staff_recruitment_applications")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_recruitment_application_no_uniq",
                def = "{'schoolId': 1, 'applicationNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vacancy_candidate_uniq",
                def = "{'schoolId': 1, 'vacancyDocsId': 1, 'candidateLookupHash': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vacancy_stage_updated_idx",
                def = "{'schoolId': 1, 'vacancyDocsId': 1, 'stage': 1, 'updatedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentApplication extends SchoolBase {

    // Generated using NumberSequenceType.RECRUITMENT_APPLICATION.
    // Example: "RAPP/2026/000001"
    @NotBlank
    private String applicationNo;

    // Links to JobVacancy.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String vacancyDocsId;

    // Encrypted JSON containing candidate name and contact/profile information.
    // Example: "kms:v1:encrypted-candidate-profile"
    @NotBlank
    private String encryptedCandidateProfile;

    // Keyed HMAC/blind index used for duplicate lookup.
    // Example: "hmac-sha256:42af..."
    @NotBlank
    private String candidateLookupHash;

    // Example: RecruitmentStage.APPLIED
    @NotNull
    @Builder.Default
    private RecruitmentStage stage = RecruitmentStage.APPLIED;

    // Example: 2026-08-05T09:30:00Z
    @NotNull
    private Instant appliedAt;

    // Links to the uploaded résumé document.
    // Example: "67aa15d9dc3f7d0022222222"
    private String resumeDocumentDocsId;

    // Example: "SCHOOL_WEBSITE"
    private String source;

    // Links to the assigned recruiter's Staff.id.
    // Example: "67aa15d9dc3f7d0033333333"
    private String assignedRecruiterDocsId;

    // Example: 82
    private Integer aggregateScore;

    // Example: "MINIMUM_QUALIFICATION_NOT_MET"
    private String rejectionReasonCode;

    // Links to Staff.id after the candidate is hired.
    // Example: "67aa15d9dc3f7d0044444444"
    private String resultingStaffDocsId;

    // Embedded interview history.
    @Builder.Default
    private List<RecruitmentInterview> interviews = new ArrayList<>();
}
