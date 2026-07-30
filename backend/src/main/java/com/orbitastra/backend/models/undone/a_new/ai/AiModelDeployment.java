package com.orbitastra.backend.models.undone.a_new.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ai_model_deployments")
@CompoundIndex(name = "tenant_model_deployment_key_uniq",
        def = "{'tenantId':1,'deploymentKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelDeployment extends TenantScopedDocument {

    private String deploymentKey;
    private String provider;
    private String modelName;
    private String modelVersion;
    private String endpointRegion;
    private String secretReference;
    private String status;
    private Integer inputRetentionDays;
    private Integer outputRetentionDays;
    private Boolean providerTrainingDisabled;
    private Boolean zeroDataRetention;
    private Instant approvedAt;
    private String approvedByDocsId;

    @Builder.Default
    private List<String> supportedCapabilities = new ArrayList<>();
}
