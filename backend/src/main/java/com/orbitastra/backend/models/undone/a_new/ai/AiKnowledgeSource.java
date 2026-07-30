package com.orbitastra.backend.models.undone.a_new.ai;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ai_knowledge_sources")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_knowledge_source_key_uniq",
                def = "{'tenantId':1,'sourceKey':1}", unique = true),
        @CompoundIndex(name = "tenant_knowledge_status_freshness_idx",
                def = "{'tenantId':1,'status':1,'lastIndexedAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeSource extends TenantScopedDocument {

    private String sourceKey;
    private String name;
    private String sourceType;
    private String sourceReference;
    private Confidentiality confidentiality;
    private String requiredPermission;
    private String dataResidencyRegion;
    private String status;
    private String indexVersion;
    private Instant lastIndexedAt;
    private Long indexedChunkCount;
    private String retentionRuleDocsId;
}
