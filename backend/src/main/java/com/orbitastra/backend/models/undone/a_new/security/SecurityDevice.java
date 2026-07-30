package com.orbitastra.backend.models.undone.a_new.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "security_devices")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_security_device_no_uniq",
                def = "{'tenantId':1,'deviceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_external_device_lookup_uniq",
                def = "{'tenantId':1,'providerKey':1,'externalDeviceId':1}", unique = true),
        @CompoundIndex(name = "tenant_security_status_health_idx",
                def = "{'tenantId':1,'status':1,'lastHealthAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityDevice extends CampusScopedDocument {

    private String deviceNo;
    private String deviceType;
    private String name;
    private String facilityResourceDocsId;
    private String providerKey;
    private String externalDeviceId;
    private String connectionSecretReference;
    private String status;
    private Instant lastHealthAt;
    private String lastHealthResult;
    private Integer mediaRetentionDays;
    private Boolean aiAnalysisEnabled;
    private Boolean privacyMaskingEnabled;

    @Builder.Default
    private List<String> capabilityCodes = new ArrayList<>();
}
