package com.orbitastra.backend.models.undone.a_new.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "webhook_subscriptions")
@CompoundIndex(name = "tenant_webhook_key_uniq",
        def = "{'tenantId':1,'subscriptionKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookSubscription extends TenantScopedDocument {

    private String subscriptionKey;
    private String integrationConnectionDocsId;
    private String endpointUrl;
    private String signingSecretReference;
    private String signatureAlgorithm;
    private Boolean active;
    private Instant disabledAt;
    private String disabledReason;

    @Builder.Default
    private List<String> eventTypes = new ArrayList<>();
}
