package com.orbitastra.backend.models.undone.a_new.aid;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "aid_applications")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_aid_application_no_uniq",
                def = "{'tenantId':1,'applicationNo':1}", unique = true),
        @CompoundIndex(name = "tenant_programme_student_year_uniq",
                def = "{'tenantId':1,'aidProgrammeDocsId':1,'studentDocsId':1,'academicYearDocsId':1}",
                unique = true),
        @CompoundIndex(name = "tenant_aid_state_submitted_idx",
                def = "{'tenantId':1,'state':1,'submittedAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AidApplication extends AcademicScopedDocument {

    private String applicationNo;
    private String aidProgrammeDocsId;
    private String studentDocsId;
    private String guardianDocsId;
    private ApprovalState state;
    private Confidentiality confidentiality;
    private Instant submittedAt;
    private String encryptedHouseholdAssessment;
    private String verificationSummary;
    private String verifiedByDocsId;
    private String committeeMeetingDocsId;
    private Integer committeeScore;
    private String decisionReason;

    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();
}
