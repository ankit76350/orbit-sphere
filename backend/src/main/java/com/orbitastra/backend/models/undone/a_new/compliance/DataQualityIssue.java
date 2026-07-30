package com.orbitastra.backend.models.undone.a_new.compliance;

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

@Document(collection = "data_quality_issues")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_rule_entity_field_open_uniq",
                def = "{'tenantId':1,'ruleKey':1,'entityType':1,'entityDocsId':1,'fieldPath':1,'active':1}",
                unique = true, partialFilter = "{'active':true}"),
        @CompoundIndex(name = "tenant_owner_status_due_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'status':1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityIssue extends TenantScopedDocument {

    private String ruleKey;
    private String entityType;
    private String entityDocsId;
    private String fieldPath;
    private String issueCode;
    private String severity;
    private Confidentiality confidentiality;
    private String status;
    private Boolean active;
    private String ownerDocsId;
    private Instant detectedAt;
    private Instant dueAt;
    private Instant resolvedAt;
    private String resolution;
}
