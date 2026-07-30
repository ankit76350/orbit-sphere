package com.orbitastra.backend.models.undone.a_new.alumni;

import java.math.BigDecimal;
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

@Document(collection = "alumni_campaigns")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_alumni_campaign_code_uniq",
                def = "{'tenantId':1,'campaignCode':1}", unique = true),
        @CompoundIndex(name = "tenant_alumni_campaign_status_end_idx",
                def = "{'tenantId':1,'status':1,'endDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniCampaign extends TenantScopedDocument {

    private String campaignCode;
    private String name;
    private String campaignType;
    private String purpose;
    private String currencyCode;
    private BigDecimal targetAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fundLedgerAccountDocsId;
    private String status;
}
