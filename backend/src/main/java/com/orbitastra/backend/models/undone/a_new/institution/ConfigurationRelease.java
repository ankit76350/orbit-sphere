package com.orbitastra.backend.models.undone.a_new.institution;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "configuration_releases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_config_key_version_uniq",
                def = "{'tenantId':1,'configurationKey':1,'scopeKey':1,'releaseVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_config_effective_idx",
                def = "{'tenantId':1,'configurationKey':1,'effectiveFrom':-1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationRelease extends TenantScopedDocument {

    private String configurationKey;
    private String scopeKey;
    private Integer releaseVersion;
    private ApprovalState state;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String approvedByDocsId;
    private Instant approvedAt;

    @Builder.Default
    private Map<String, Object> configuration = new HashMap<>();
}
