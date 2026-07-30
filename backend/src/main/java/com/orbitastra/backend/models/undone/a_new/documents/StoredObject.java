package com.orbitastra.backend.models.undone.a_new.documents;

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

@Document(collection = "stored_objects")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_object_key_uniq",
                def = "{'tenantId':1,'objectKey':1}", unique = true),
        @CompoundIndex(name = "tenant_content_hash_idx",
                def = "{'tenantId':1,'contentHash':1}"),
        @CompoundIndex(name = "tenant_scan_status_idx",
                def = "{'tenantId':1,'malwareScanStatus':1,'createdAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StoredObject extends TenantScopedDocument {

    private String objectKey;
    private String storageProvider;
    private String storageRegion;
    private String bucketReference;
    private String storagePath;
    private String originalFileName;
    private String mediaType;
    private Long sizeBytes;
    private String contentHash;
    private String encryptionKeyReference;
    private Confidentiality confidentiality;
    private String malwareScanStatus;
    private Instant malwareScannedAt;
    private String retentionRuleDocsId;
    private String legalHoldDocsId;
    private Instant purgeEligibleAt;
}
