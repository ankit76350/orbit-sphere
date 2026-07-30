package com.orbitastra.backend.models.undone.a_new.governance;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "governance_actions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_resolution_no_uniq",
                def = "{'tenantId':1,'resolutionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_owner_state_due_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'state':1,'dueDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceAction extends TenantScopedDocument {

    private String meetingDocsId;
    private String resolutionNo;
    private String title;
    private String decisionText;
    private String ownerDocsId;
    private LocalDate dueDate;
    private ApprovalState state;
    private String evidenceDocumentDocsId;
    private String closureRemarks;
}
