package com.orbitastra.backend.models.undone.a_new.conduct;

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

@Document(collection = "conduct_actions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_conduct_case_action_seq_uniq",
                def = "{'tenantId':1,'studentConductCaseDocsId':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_conduct_owner_status_due_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConductAction extends TenantScopedDocument {

    private String studentConductCaseDocsId;
    private Integer sequenceNo;
    private String actionType;
    private String description;
    private String ownerDocsId;
    private String status;
    private Instant dueAt;
    private Instant completedAt;
    private String completionEvidenceDocumentDocsId;
    private String approvedByDocsId;
}
