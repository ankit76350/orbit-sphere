package com.orbitastra.backend.models.undone.a_new.integration;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Durable inbox/outbox record. Payloads larger than the configured threshold are
 * stored in object storage and referenced by {@code payloadDocumentDocsId}.
 */
@Document(collection = "integration_messages")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_connection_idempotency_uniq",
                def = "{'tenantId':1,'integrationConnectionDocsId':1,'direction':1,'idempotencyKey':1}", unique = true),
        @CompoundIndex(name = "tenant_message_status_retry_idx",
                def = "{'tenantId':1,'status':1,'nextAttemptAt':1}"),
        @CompoundIndex(name = "tenant_correlation_time_idx",
                def = "{'tenantId':1,'correlationId':1,'createdAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMessage extends TenantScopedDocument {

    private String integrationConnectionDocsId;
    private String integrationJobDocsId;
    private String direction;
    private String messageType;
    private String idempotencyKey;
    private String correlationId;
    private String payloadHash;
    private String payloadDocumentDocsId;
    private String status;
    private Integer attemptCount;
    private Instant nextAttemptAt;
    private Instant processedAt;
    private String errorCode;
    private String errorMessage;

    @Indexed(expireAfter = "0s")
    private Instant expireAt;
}
