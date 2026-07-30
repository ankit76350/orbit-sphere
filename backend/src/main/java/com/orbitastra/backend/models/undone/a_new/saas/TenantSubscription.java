package com.orbitastra.backend.models.undone.a_new.saas;

import java.math.BigDecimal;
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

@Document(collection = "tenant_subscriptions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_subscription_no_uniq",
                def = "{'tenantId':1,'subscriptionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_subscription_status_period_idx",
                def = "{'tenantId':1,'status':1,'currentPeriodEnd':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscription extends TenantScopedDocument {

    private String subscriptionNo;
    private String planDefinitionDocsId;
    private Integer planVersion;
    private String status;
    private Instant trialEndsAt;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Boolean autoRenew;
    private BigDecimal contractedPrice;
    private String currencyCode;
    private String billingCustomerReference;
    private String contractDocsId;
    private Instant cancelledAt;
    private String cancellationReason;
}
