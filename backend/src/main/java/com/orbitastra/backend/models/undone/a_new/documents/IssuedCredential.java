package com.orbitastra.backend.models.undone.a_new.documents;

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

@Document(collection = "issued_credentials")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_credential_no_uniq",
                def = "{'tenantId':1,'credentialNo':1}", unique = true),
        @CompoundIndex(name = "tenant_credential_verification_hash_uniq",
                def = "{'tenantId':1,'verificationCodeHash':1}", unique = true),
        @CompoundIndex(name = "tenant_credential_holder_type_status_idx",
                def = "{'tenantId':1,'holderDocsId':1,'credentialType':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IssuedCredential extends TenantScopedDocument {

    private String credentialNo;
    private String credentialType;
    private String holderType;
    private String holderDocsId;
    private String academicYearDocsId;
    private String documentRecordDocsId;
    private String verificationCodeHash;
    private String machineReadablePayload;
    private String status;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String revocationReason;
}
