package com.orbitastra.backend.models.undone.a_new.ai;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ai_runs")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_ai_run_no_uniq",
                def = "{'tenantId':1,'runNo':1}", unique = true),
        @CompoundIndex(name = "tenant_use_case_started_idx",
                def = "{'tenantId':1,'aiUseCaseDocsId':1,'startedAt':-1}"),
        @CompoundIndex(name = "tenant_actor_started_idx",
                def = "{'tenantId':1,'actorDocsId':1,'startedAt':-1}"),
        @CompoundIndex(name = "tenant_ai_status_review_idx",
                def = "{'tenantId':1,'status':1,'humanReviewRequired':1,'completedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiRun extends TenantScopedDocument {

    private String runNo;
    private String aiUseCaseDocsId;
    private String modelDeploymentDocsId;
    private String promptVersionDocsId;
    private String actorDocsId;
    private String actorRoleKey;
    private String subjectType;
    private String subjectDocsId;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private Long inputTokens;
    private Long outputTokens;
    private BigDecimal estimatedCost;
    private String currencyCode;
    private String promptHash;
    private String responseHash;
    private String protectedPromptDocumentDocsId;
    private String protectedResponseDocumentDocsId;
    private Boolean piiMinimizationApplied;
    private Boolean humanReviewRequired;
    private String correlationId;
    private String errorCode;

    @Builder.Default
    private List<String> knowledgeSourceDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> dataCategoryKeys = new ArrayList<>();
}
