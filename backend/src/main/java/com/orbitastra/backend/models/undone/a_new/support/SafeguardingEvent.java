package com.orbitastra.backend.models.undone.a_new.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

@Document(collection = "safeguarding_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_safeguarding_case_event_time_idx",
                def = "{'tenantId':1,'safeguardingCaseDocsId':1,'occurredAt':1}"),
        @CompoundIndex(name = "tenant_safeguarding_action_due_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'actionStatus':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SafeguardingEvent extends TenantScopedDocument {

    private String safeguardingCaseDocsId;
    private String eventType;
    private Instant occurredAt;
    private String recordedByDocsId;
    private Confidentiality confidentiality;
    private String encryptedNarrative;
    private String agencyName;
    private String agencyReference;
    private String ownerDocsId;
    private Instant dueAt;
    private String actionStatus;

    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();
}
