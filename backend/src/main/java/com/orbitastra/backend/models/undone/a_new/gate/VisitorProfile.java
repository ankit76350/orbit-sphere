package com.orbitastra.backend.models.undone.a_new.gate;

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

@Document(collection = "visitor_profiles")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_visitor_identity_lookup_uniq",
                def = "{'tenantId':1,'identityLookupHash':1}", unique = true,
                partialFilter = "{'identityLookupHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_visitor_contact_lookup_idx",
                def = "{'tenantId':1,'contactLookupHash':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorProfile extends TenantScopedDocument {

    private String displayName;
    private String organizationName;
    private String encryptedContact;
    private String contactLookupHash;
    private String encryptedIdentityEvidence;
    private String identityLookupHash;
    private String photoStoredObjectDocsId;
    private String verificationStatus;
    private String watchlistStatus;
    private Instant lastVerifiedAt;
}
