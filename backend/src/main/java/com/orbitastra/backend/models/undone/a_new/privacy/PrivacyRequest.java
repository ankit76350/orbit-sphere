package com.orbitastra.backend.models.undone.a_new.privacy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "privacy_requests")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_privacy_request_no_uniq",
                def = "{'tenantId':1,'requestNo':1}", unique = true),
        @CompoundIndex(name = "tenant_privacy_owner_status_due_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyRequest extends TenantScopedDocument {

    private String requestNo;
    private String requestType;
    private PersonType subjectType;
    private String subjectDocsId;
    private String requesterDocsId;
    private String requesterRelationship;
    private String status;
    private String ownerDocsId;
    private Instant receivedAt;
    private Instant identityVerifiedAt;
    private Instant dueAt;
    private Instant completedAt;
    private String decisionReason;
    private String responseDocumentDocsId;
    private String legalHoldDocsId;

    @Builder.Default
    private List<String> scopedDataDomains = new ArrayList<>();
}
