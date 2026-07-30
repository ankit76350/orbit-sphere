package com.orbitastra.backend.models.undone.a_new.it;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "managed_devices")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_device_no_uniq",
                def = "{'tenantId':1,'deviceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_hardware_lookup_uniq",
                def = "{'tenantId':1,'hardwareIdLookupHash':1}", unique = true,
                partialFilter = "{'hardwareIdLookupHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_device_status_checkin_idx",
                def = "{'tenantId':1,'status':1,'lastCheckInAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ManagedDevice extends CampusScopedDocument {

    private String deviceNo;
    private String assetDocsId;
    private String deviceType;
    private String manufacturer;
    private String model;
    private String encryptedHardwareIdentifiers;
    private String hardwareIdLookupHash;
    private String operatingSystem;
    private String operatingSystemVersion;
    private String assignedToType;
    private String assignedToDocsId;
    private String mdmProvider;
    private String mdmExternalId;
    private String status;
    private Boolean encryptedStorage;
    private Boolean compliant;
    private Instant lastCheckInAt;
    private LocalDate warrantyUntil;
}
