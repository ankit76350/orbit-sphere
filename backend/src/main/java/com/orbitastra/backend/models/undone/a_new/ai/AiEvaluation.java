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

@Document(collection = "ai_evaluations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_evaluation_no_uniq",
                def = "{'tenantId':1,'evaluationNo':1}", unique = true),
        @CompoundIndex(name = "tenant_use_case_model_eval_idx",
                def = "{'tenantId':1,'aiUseCaseDocsId':1,'modelDeploymentDocsId':1,'executedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvaluation extends TenantScopedDocument {

    private String evaluationNo;
    private String aiUseCaseDocsId;
    private String modelDeploymentDocsId;
    private String promptVersionDocsId;
    private String evaluationSuiteKey;
    private String evaluationSuiteVersion;
    private Instant executedAt;
    private String status;
    private BigDecimal overallScore;
    private Boolean deploymentGatePassed;
    private String reportDocumentDocsId;

    @Builder.Default
    private List<MetricResult> metrics = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResult {
        private String metricKey;
        private BigDecimal score;
        private BigDecimal threshold;
        private Boolean passed;
    }
}
