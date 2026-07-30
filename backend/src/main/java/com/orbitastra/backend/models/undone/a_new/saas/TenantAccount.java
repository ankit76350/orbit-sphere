package com.orbitastra.backend.models.undone.a_new.saas;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AuditedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "tenant_accounts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_account_code_uniq", def = "{'tenantCode':1}", unique = true),
        @CompoundIndex(name = "tenant_account_slug_uniq", def = "{'slug':1}", unique = true),
        @CompoundIndex(name = "tenant_region_status_idx", def = "{'hostingRegion':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccount extends AuditedDocument {

    public enum TenantStatus {
        TRIAL,
        PROVISIONING,
        ACTIVE,
        SUSPENDED,
        OFFBOARDING,
        CLOSED,
        DELETION_PENDING,
        DELETED
    }

    private String slug;
    private String hostingRegion;
    private String dataResidencyPolicy;
    private String databasePlacementKey;
    private Instant deletionEligibleAt;

}
