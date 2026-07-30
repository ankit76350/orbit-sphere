package com.orbitastra.backend.models.undone.a_new.people;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

@Document(collection = "performance_assessments")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_cycle_subject_respondent_uniq",
                def = "{'tenantId':1,'performanceCycleDocsId':1,'subjectDocsId':1,'respondentType':1,'respondentLookupHash':1}",
                unique = true),
        @CompoundIndex(name = "tenant_cycle_subject_status_idx",
                def = "{'tenantId':1,'performanceCycleDocsId':1,'subjectDocsId':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAssessment extends TenantScopedDocument {

    private String performanceCycleDocsId;
    private String subjectType;
    private String subjectDocsId;
    private String respondentType;
    private String respondentDocsId;
    private String respondentLookupHash;
    private Confidentiality confidentiality;
    private String status;
    private Instant submittedAt;
    private BigDecimal weightedScore;
    private String narrative;

    @Builder.Default
    private Map<String, BigDecimal> criterionScores = new HashMap<>();
}
