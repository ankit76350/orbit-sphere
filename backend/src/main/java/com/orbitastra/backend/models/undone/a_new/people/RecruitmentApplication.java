package com.orbitastra.backend.models.undone.a_new.people;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "recruitment_applications")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_recruitment_application_no_uniq",
                def = "{'tenantId':1,'applicationNo':1}", unique = true),
        @CompoundIndex(name = "tenant_vacancy_candidate_lookup_uniq",
                def = "{'tenantId':1,'vacancyDocsId':1,'candidateLookupHash':1}", unique = true),
        @CompoundIndex(name = "tenant_vacancy_stage_updated_idx",
                def = "{'tenantId':1,'vacancyDocsId':1,'stage':1,'updatedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentApplication extends TenantScopedDocument {

    public enum RecruitmentStage {
        APPLIED,
        SCREENING,
        SHORTLISTED,
        INTERVIEW,
        VERIFICATION,
        OFFERED,
        ACCEPTED,
        REJECTED,
        WITHDRAWN,
        HIRED
    }

    private String applicationNo;
    private String vacancyDocsId;
    private String encryptedCandidateProfile;
    private String candidateLookupHash;
    private Confidentiality confidentiality;
    private RecruitmentStage stage;
    private Instant appliedAt;
    private String resumeDocumentDocsId;
    private String source;
    private String assignedRecruiterDocsId;
    private Integer aggregateScore;
    private String rejectionReasonCode;
    private String resultingStaffDocsId;

    @Builder.Default
    private List<Interview> interviews = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interview {
        private String interviewKey;
        private Instant scheduledAt;
        private List<String> panelistDocsIds;
        private Integer score;
        private String recommendation;
        private String scorecardDocumentDocsId;
    }
}
