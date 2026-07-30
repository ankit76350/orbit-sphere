package com.orbitastra.backend.models.undone.a_new.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "workflow_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_workflow_key_version_uniq",
                def = "{'tenantId':1,'workflowKey':1,'workflowVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_workflow_state_effective_idx",
                def = "{'tenantId':1,'workflowKey':1,'state':1,'effectiveFrom':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition extends TenantScopedDocument {

    private String workflowKey;
    private Integer workflowVersion;
    private String name;
    private String entityType;
    private String formDefinitionDocsId;
    private ApprovalState state;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String initialStateKey;

    @Builder.Default
    private List<StateDefinition> states = new ArrayList<>();

    @Builder.Default
    private List<TransitionDefinition> transitions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateDefinition {
        private String stateKey;
        private String label;
        private Boolean terminal;
        private Integer slaMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionDefinition {
        private String transitionKey;
        private String fromStateKey;
        private String toStateKey;
        private String permission;
        private String guardExpression;
        private Boolean approvalRequired;
        private String assigneeExpression;
    }
}
