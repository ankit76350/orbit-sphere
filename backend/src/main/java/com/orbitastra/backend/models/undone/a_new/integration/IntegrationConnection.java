package com.orbitastra.backend.models.undone.a_new.integration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "integration_connections")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_connection_key_uniq",
                def = "{'tenantId':1,'connectionKey':1}", unique = true),
        @CompoundIndex(name = "tenant_connector_status_idx",
                def = "{'tenantId':1,'connectorKey':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationConnection extends TenantScopedDocument {

    private String connectionKey;
    private String connectorKey;
    private String connectorVersion;
    private String scopeKey;
    private String status;
    private String secretReference;
    private String externalTenantReference;
    private String dataResidencyRegion;
    private Instant lastHealthCheckAt;
    private String lastHealthStatus;

    /** Non-secret, schema-validated connector settings only. */
    @Builder.Default
    private Map<String, Object> settings = new HashMap<>();

    @Builder.Default
    private Map<String, String> fieldMappings = new HashMap<>();
}
