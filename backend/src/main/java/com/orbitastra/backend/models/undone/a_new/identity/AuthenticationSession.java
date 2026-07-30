package com.orbitastra.backend.models.undone.a_new.identity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "authentication_sessions")
@CompoundIndex(name = "tenant_account_session_idx",
        def = "{'tenantId':1,'identityAccountDocsId':1,'revokedAt':1,'expiresAt':1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationSession extends TenantScopedDocument {

    private String identityAccountDocsId;
    private String refreshTokenHash;
    private String deviceIdHash;
    private String deviceName;
    private String userAgent;
    private String ipAddress;
    private Instant authenticatedAt;
    private Instant lastSeenAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private Instant revokedAt;
    private String revokedByDocsId;
    private String revokeReason;
}
