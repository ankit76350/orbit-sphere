package com.orbitastra.backend.models.undone.a_new.privacy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "data_protection_assessments")
@CompoundIndex(name = "tenant_assessment_key_version_uniq",
        def = "{'tenantId':1,'assessmentKey':1,'assessmentVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DataProtectionAssessment extends TenantScopedDocument {

    private String assessmentKey;
    private Integer assessmentVersion;
    private String processingActivityDocsId;
    private String aiUseCaseDocsId;
    private String title;
    private ApprovalState state;
    private String ownerDocsId;
    private String reviewerDocsId;
    private String inherentRisk;
    private String residualRisk;
    private LocalDate nextReviewDate;
    private String reportDocumentDocsId;

    @Builder.Default
    private List<String> mitigationActions = new ArrayList<>();
}
