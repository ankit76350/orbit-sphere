package com.orbitastra.backend.models.undone.a_new.workflow;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

@Document(collection = "workflow_runs")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_workflow_run_no_uniq",
                def = "{'tenantId':1,'runNo':1}", unique = true),
        @CompoundIndex(name = "tenant_entity_workflow_active_idx",
                def = "{'tenantId':1,'entityType':1,'entityDocsId':1,'status':1}"),
        @CompoundIndex(name = "tenant_workflow_state_sla_idx",
                def = "{'tenantId':1,'workflowDefinitionDocsId':1,'currentStateKey':1,'slaDueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRun extends TenantScopedDocument {

    private String runNo;
    private String workflowDefinitionDocsId;
    private Integer workflowVersion;
    private String entityType;
    private String entityDocsId;
    private String currentStateKey;
    private String status;
    private String initiatedByDocsId;
    private Instant startedAt;
    private Instant slaDueAt;
    private Instant completedAt;
    private String idempotencyKey;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
}
