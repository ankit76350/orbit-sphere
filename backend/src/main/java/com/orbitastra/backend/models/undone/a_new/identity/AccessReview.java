package com.orbitastra.backend.models.undone.a_new.identity;

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

@Document(collection = "access_reviews")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_review_assignment_cycle_uniq",
                def = "{'tenantId':1,'reviewCycleKey':1,'accessAssignmentDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_reviewer_status_due_idx",
                def = "{'tenantId':1,'reviewerDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AccessReview extends TenantScopedDocument {

    public enum ReviewStatus {
        PENDING,
        CERTIFIED,
        MODIFIED,
        REVOKED,
        EXPIRED
    }

    private String reviewCycleKey;
    private String accessAssignmentDocsId;
    private String reviewerDocsId;
    private ReviewStatus status;
    private Instant dueAt;
    private Instant decidedAt;
    private String decisionReason;
}
