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

@Document(collection = "tenant_domains")
@CompoundIndexes({
        @CompoundIndex(name = "hostname_uniq", def = "{'normalizedHostname':1}", unique = true),
        @CompoundIndex(name = "tenant_domain_status_idx", def = "{'tenantId':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDomain extends TenantScopedDocument {

    private String normalizedHostname;
    private String status;
    private String verificationTokenHash;
    private Instant verifiedAt;
    private String certificateReference;
    private Instant certificateExpiresAt;
    private Boolean primary;
}
