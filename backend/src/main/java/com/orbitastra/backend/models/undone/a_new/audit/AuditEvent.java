package com.orbitastra.backend.models.undone.a_new.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

/**
 * Append-only security and business audit event. Application code must prohibit
 * update/delete; archival should move signed batches to immutable storage.
 */
@Document(collection = "audit_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_audit_sequence_uniq",
                def = "{'tenantId':1,'partitionKey':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_actor_time_idx",
                def = "{'tenantId':1,'actorDocsId':1,'occurredAt':-1}"),
        @CompoundIndex(name = "tenant_entity_time_idx",
                def = "{'tenantId':1,'entityType':1,'entityDocsId':1,'occurredAt':-1}"),
        @CompoundIndex(name = "tenant_action_time_idx",
                def = "{'tenantId':1,'action':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent extends TenantScopedDocument {

    private String partitionKey;
    private Long sequenceNo;
    private Instant occurredAt;
    private String actorType;
    private String actorDocsId;
    private String actorRoleKey;
    private String action;
    private String outcome;
    private String entityType;
    private String entityDocsId;
    private String campusDocsId;
    private String correlationId;
    private String requestId;
    private String ipAddress;
    private String userAgent;
    private Confidentiality confidentiality;
    private String previousEventHash;
    private String eventHash;

    @Builder.Default
    private Map<String, Object> safeMetadata = new HashMap<>();
}
