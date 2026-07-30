package com.orbitastra.backend.models.undone.a_new.identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "support_access_sessions")
@CompoundIndex(name = "tenant_support_state_expiry_idx",
        def = "{'tenantId':1,'state':1,'expiresAt':1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAccessSession extends TenantScopedDocument {

    private String supportEngineerPrincipal;
    private String requestedByPrincipal;
    private String approvedByDocsId;
    private ApprovalState state;
    private String ticketNo;
    private String purpose;
    private Instant startsAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private Boolean customerVisible;
    private Boolean recordingRequired;

    @Builder.Default
    private List<String> allowedResources = new ArrayList<>();
}
