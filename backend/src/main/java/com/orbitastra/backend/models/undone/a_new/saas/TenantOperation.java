package com.orbitastra.backend.models.undone.a_new.saas;

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

@Document(collection = "tenant_operations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_operation_no_uniq",
                def = "{'tenantId':1,'operationNo':1}", unique = true),
        @CompoundIndex(name = "tenant_operation_status_idx",
                def = "{'tenantId':1,'operationType':1,'status':1,'requestedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantOperation extends TenantScopedDocument {

    private String operationNo;
    private String operationType;
    private String status;
    private String requestedByPrincipal;
    private String approvedByPrincipal;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;
    private String sourceRegion;
    private String targetRegion;
    private String exportDocumentDocsId;
    private String backupSnapshotDocsId;
    private String errorCode;
    private String errorMessage;
}
