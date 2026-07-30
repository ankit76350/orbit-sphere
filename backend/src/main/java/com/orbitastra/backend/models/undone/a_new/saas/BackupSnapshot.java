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

@Document(collection = "backup_snapshots")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_snapshot_no_uniq",
                def = "{'tenantId':1,'snapshotNo':1}", unique = true),
        @CompoundIndex(name = "tenant_snapshot_status_time_idx",
                def = "{'tenantId':1,'status':1,'snapshotAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BackupSnapshot extends TenantScopedDocument {

    private String snapshotNo;
    private String snapshotType;
    private String hostingRegion;
    private String storageReference;
    private String encryptionKeyReference;
    private String checksum;
    private Instant snapshotAt;
    private Instant recoverableUntil;
    private String status;
    private Boolean restoreTested;
    private Instant lastRestoreTestAt;
}
