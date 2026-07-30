package com.orbitastra.backend.models.undone.a_new.identity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ScopeType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "access_assignments")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_principal_role_scope_uniq",
                def = "{'tenantId':1,'principalDocsId':1,'roleDocsId':1,'scopeType':1,'scopeDocsId':1}",
                unique = true),
        @CompoundIndex(name = "tenant_scope_active_idx",
                def = "{'tenantId':1,'scopeType':1,'scopeDocsId':1,'active':1}"),
        @CompoundIndex(name = "tenant_principal_active_idx",
                def = "{'tenantId':1,'principalDocsId':1,'active':1,'validUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AccessAssignment extends TenantScopedDocument {

    private String principalDocsId;
    private String roleDocsId;
    private ScopeType scopeType;
    private String scopeDocsId;
    private Instant validFrom;
    private Instant validUntil;
    private Boolean active;
    private String approvalRequestDocsId;
    private String reason;
}
