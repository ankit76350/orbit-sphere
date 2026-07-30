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

@Document(collection = "workflow_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_run_sequence_uniq",
                def = "{'tenantId':1,'workflowRunDocsId':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_workflow_event_time_idx",
                def = "{'tenantId':1,'eventType':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEvent extends TenantScopedDocument {

    private String workflowRunDocsId;
    private Long sequenceNo;
    private String eventType;
    private String fromStateKey;
    private String toStateKey;
    private String actorDocsId;
    private Instant occurredAt;
    private String correlationId;

    @Builder.Default
    private Map<String, Object> eventData = new HashMap<>();
}
