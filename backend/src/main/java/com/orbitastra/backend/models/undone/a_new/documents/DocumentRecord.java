package com.orbitastra.backend.models.undone.a_new.documents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "document_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_document_no_version_uniq",
                def = "{'tenantId':1,'documentNo':1,'documentVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_entity_document_idx",
                def = "{'tenantId':1,'entityType':1,'entityDocsId':1,'category':1,'createdAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRecord extends TenantScopedDocument {

    private String documentNo;
    private Integer documentVersion;
    private String category;
    private String title;
    private String entityType;
    private String entityDocsId;
    private String storedObjectDocsId;
    private ApprovalState state;
    private Confidentiality confidentiality;
    private String supersedesDocumentDocsId;
    private String verificationCodeHash;
    private String integrityHash;
    private Instant validFrom;
    private Instant validUntil;
    private Instant revokedAt;
    private String revocationReason;

    @Builder.Default
    private List<Signature> signatures = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Signature {
        private String signerDocsId;
        private String signatureProvider;
        private String signatureReference;
        private Instant signedAt;
        private String certificateFingerprint;
    }
}
