package com.orbitastra.backend.models.undone.a_new.workflow;

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

@Document(collection = "workflow_tasks")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_run_task_key_uniq",
                def = "{'tenantId':1,'workflowRunDocsId':1,'taskKey':1}", unique = true),
        @CompoundIndex(name = "tenant_assignee_status_due_idx",
                def = "{'tenantId':1,'assigneeDocsId':1,'status':1,'dueAt':1}"),
        @CompoundIndex(name = "tenant_role_status_due_idx",
                def = "{'tenantId':1,'assigneeRoleKey':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTask extends TenantScopedDocument {

    private String workflowRunDocsId;
    private String taskKey;
    private String title;
    private String status;
    private String assigneeDocsId;
    private String assigneeRoleKey;
    private Instant dueAt;
    private Integer escalationLevel;
    private String delegatedFromDocsId;
    private Instant completedAt;
    private String outcome;
    private String comment;
}
