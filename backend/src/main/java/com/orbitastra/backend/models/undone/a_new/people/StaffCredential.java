package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "staff_credentials")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_staff_credential_uniq",
                def = "{'tenantId':1,'staffDocsId':1,'credentialType':1,'credentialNoLookupHash':1}", unique = true),
        @CompoundIndex(name = "tenant_credential_expiry_idx",
                def = "{'tenantId':1,'verificationStatus':1,'validUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCredential extends TenantScopedDocument {

    private String staffDocsId;
    private String credentialType;
    private String title;
    private String issuingAuthority;
    private String encryptedCredentialNo;
    private String credentialNoLookupHash;
    private LocalDate issuedOn;
    private LocalDate validUntil;
    private String verificationStatus;
    private String verifiedByDocsId;
    private String evidenceDocumentDocsId;
}
