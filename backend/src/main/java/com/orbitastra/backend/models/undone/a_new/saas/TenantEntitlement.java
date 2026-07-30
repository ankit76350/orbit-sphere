package com.orbitastra.backend.models.undone.a_new.saas;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "tenant_entitlements")
@CompoundIndex(name = "tenant_feature_uniq",
        def = "{'tenantId':1,'featureKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEntitlement extends TenantScopedDocument {

    private String featureKey;
    private String source;
    private String subscriptionDocsId;
    private Boolean enabled;
    private Long quota;
    private String quotaUnit;
    private String overagePolicy;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
}
