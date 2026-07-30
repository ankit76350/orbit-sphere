package com.orbitastra.backend.models.undone.a_new.communication;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "delivery_endpoints")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_endpoint_lookup_uniq",
                def = "{'tenantId':1,'channel':1,'endpointLookupHash':1}", unique = true),
        @CompoundIndex(name = "tenant_person_channel_idx",
                def = "{'tenantId':1,'personType':1,'personDocsId':1,'channel':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEndpoint extends TenantScopedDocument {

    private PersonType personType;
    private String personDocsId;
    private String channel;
    private String encryptedEndpoint;
    private String endpointLookupHash;
    private String status;
    private Boolean primary;
    private Instant verifiedAt;
    private Instant bouncedAt;
    private String providerReference;
}
