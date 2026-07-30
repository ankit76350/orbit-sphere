package com.orbitastra.backend.models.undone.a_new.identity;

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

@Document(collection = "authenticators")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_account_authenticator_uniq",
                def = "{'tenantId':1,'identityAccountDocsId':1,'credentialIdHash':1}", unique = true),
        @CompoundIndex(name = "tenant_account_authenticator_idx",
                def = "{'tenantId':1,'identityAccountDocsId':1,'active':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Authenticator extends TenantScopedDocument {

    public enum AuthenticatorType {
        PASSWORD,
        PASSKEY,
        TOTP,
        RECOVERY_CODE_SET,
        FEDERATED
    }

    private String identityAccountDocsId;
    private AuthenticatorType type;
    private String credentialIdHash;

    /**
     * Password hashes or KMS/Vault secret references only. TOTP secrets and
     * private keys must never be persisted as plaintext in MongoDB.
     */
    private String protectedCredential;

    private String algorithm;
    private String label;
    private Boolean active;
    private Instant verifiedAt;
    private Instant lastUsedAt;
}
