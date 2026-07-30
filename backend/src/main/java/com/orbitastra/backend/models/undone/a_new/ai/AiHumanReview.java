package com.orbitastra.backend.models.undone.a_new.ai;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ai_human_reviews")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_ai_run_review_version_uniq",
                def = "{'tenantId':1,'aiRunDocsId':1,'reviewVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_reviewer_status_due_idx",
                def = "{'tenantId':1,'reviewerDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiHumanReview extends TenantScopedDocument {

    private String aiRunDocsId;
    private Integer reviewVersion;
    private String reviewerDocsId;
    private String status;
    private Instant dueAt;
    private Instant reviewedAt;
    private String decision;
    private String correctionSummary;
    private String approvedOutputDocumentDocsId;
    private Boolean appealRequested;
    private String appealOutcome;
}
