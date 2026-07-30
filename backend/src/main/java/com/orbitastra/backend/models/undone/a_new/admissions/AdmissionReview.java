package com.orbitastra.backend.models.undone.a_new.admissions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "admission_reviews")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_application_review_round_reviewer_uniq",
                def = "{'tenantId':1,'admissionApplicationDocsId':1,'reviewRound':1,'reviewerDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_reviewer_status_due_idx",
                def = "{'tenantId':1,'reviewerDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionReview extends AcademicScopedDocument {

    private String admissionApplicationDocsId;
    private Integer reviewRound;
    private String reviewerDocsId;
    private String reviewerRoleKey;
    private String status;
    private Instant dueAt;
    private Instant completedAt;
    private BigDecimal score;
    private String recommendation;
    private String encryptedNotes;
    private Confidentiality confidentiality;

    @Builder.Default
    private Map<String, BigDecimal> criterionScores = new HashMap<>();
}
