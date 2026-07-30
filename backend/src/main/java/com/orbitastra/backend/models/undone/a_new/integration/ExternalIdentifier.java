package com.orbitastra.backend.models.undone.a_new.integration;

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

@Document(collection = "external_identifiers")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_connection_external_uniq",
                def = "{'tenantId':1,'integrationConnectionDocsId':1,'entityType':1,'externalId':1}", unique = true),
        @CompoundIndex(name = "tenant_connection_internal_uniq",
                def = "{'tenantId':1,'integrationConnectionDocsId':1,'entityType':1,'internalDocsId':1}", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalIdentifier extends TenantScopedDocument {

    private String integrationConnectionDocsId;
    private String entityType;
    private String internalDocsId;
    private String externalId;
    private String externalParentId;
    private String externalVersion;
    private Instant lastSynchronizedAt;
    private String syncState;
}
