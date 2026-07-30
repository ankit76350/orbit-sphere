package com.orbitastra.backend.models.undone.a_new.documents;

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

@Document(collection = "document_generation_jobs")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_document_generation_idempotency_uniq",
                def = "{'tenantId':1,'idempotencyKey':1}", unique = true),
        @CompoundIndex(name = "tenant_document_generation_status_next_idx",
                def = "{'tenantId':1,'status':1,'nextAttemptAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationJob extends TenantScopedDocument {

    private String idempotencyKey;
    private String documentTemplateDefinitionDocsId;
    private String entityType;
    private String entityDocsId;
    private String requestedByDocsId;
    private String outputLocale;
    private String status;
    private Integer attemptCount;
    private Instant nextAttemptAt;
    private Instant startedAt;
    private Instant completedAt;
    private String generatedDocumentRecordDocsId;
    private String inputSnapshotHash;
    private String failureCode;
    private String failureMessageRedacted;
}
