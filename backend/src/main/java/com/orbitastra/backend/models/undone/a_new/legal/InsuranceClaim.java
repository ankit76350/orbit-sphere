package com.orbitastra.backend.models.undone.a_new.legal;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "insurance_claims")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_claim_no_uniq",
                def = "{'tenantId':1,'claimNo':1}", unique = true),
        @CompoundIndex(name = "tenant_policy_claim_status_idx",
                def = "{'tenantId':1,'insurancePolicyDocsId':1,'status':1,'incidentAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceClaim extends TenantScopedDocument {

    private String claimNo;
    private String insurancePolicyDocsId;
    private String externalClaimNo;
    private Instant incidentAt;
    private String incidentType;
    private String relatedEntityType;
    private String relatedEntityDocsId;
    private Confidentiality confidentiality;
    private BigDecimal claimedAmount;
    private BigDecimal settledAmount;
    private String currencyCode;
    private String status;
    private String ownerDocsId;
    private String claimDocumentDocsId;
}
