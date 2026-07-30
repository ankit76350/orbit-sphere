package com.orbitastra.backend.models.undone.a_new.identity;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "identity_accounts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_username_uniq",
                def = "{'tenantId':1,'normalizedUsername':1}", unique = true),
        @CompoundIndex(name = "tenant_email_lookup_uniq",
                def = "{'tenantId':1,'emailLookupHash':1}", unique = true,
                partialFilter = "{'emailLookupHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_person_ref_uniq",
                def = "{'tenantId':1,'personType':1,'personDocsId':1}", unique = true,
                partialFilter = "{'personDocsId':{'$type':'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityAccount extends TenantScopedDocument {

    public enum AccountStatus {
        INVITED,
        ACTIVE,
        LOCKED,
        SUSPENDED,
        DEPROVISIONED
    }

    private String normalizedUsername;
    private String displayName;

    /** Application-encrypted value, never plaintext. */
    private String encryptedPrimaryEmail;

    /** Keyed HMAC blind index used only for equality lookup. */
    private String emailLookupHash;

    private String encryptedPrimaryPhone;
    private String phoneLookupHash;
    private PersonType personType;
    private String personDocsId;
    private AccountStatus status;
    private String locale;
    private ZoneId timeZone;
    private Boolean mfaRequired;
    private Instant passwordChangedAt;
    private Instant lastLoginAt;
    private Integer failedLoginCount;
    private Instant lockedUntil;

    @Builder.Default
    private List<FederatedSubject> federatedSubjects = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FederatedSubject {
        private String providerKey;
        private String issuer;
        private String subject;
        private Instant linkedAt;
    }
}
