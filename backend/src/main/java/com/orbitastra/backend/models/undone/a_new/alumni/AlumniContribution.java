package com.orbitastra.backend.models.undone.a_new.alumni;

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

@Document(collection = "alumni_contributions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_alumni_contribution_no_uniq",
                def = "{'tenantId':1,'contributionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_alumni_campaign_received_idx",
                def = "{'tenantId':1,'alumniCampaignDocsId':1,'receivedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniContribution extends TenantScopedDocument {

    private String contributionNo;
    private String alumniCampaignDocsId;
    private String alumniProfileDocsId;
    private String contributorPersonDocsId;
    private String contributionType;
    private String currencyCode;
    private BigDecimal amount;
    private Boolean anonymous;
    private String paymentTransactionDocsId;
    private String receiptDocumentDocsId;
    private String status;
    private Instant receivedAt;
}
