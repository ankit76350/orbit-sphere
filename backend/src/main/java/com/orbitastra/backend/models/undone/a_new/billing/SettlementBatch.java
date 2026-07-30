package com.orbitastra.backend.models.undone.a_new.billing;

import java.math.BigDecimal;
import java.time.Instant;
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

@Document(collection = "settlement_batches")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_provider_settlement_ref_uniq",
                def = "{'tenantId':1,'providerKey':1,'providerSettlementReference':1}", unique = true),
        @CompoundIndex(name = "tenant_settlement_status_date_idx",
                def = "{'tenantId':1,'status':1,'settlementDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementBatch extends TenantScopedDocument {

    private String providerKey;
    private String providerSettlementReference;
    private LocalDate settlementDate;
    private String currencyCode;
    private BigDecimal grossAmount;
    private BigDecimal feeAmount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private Integer transactionCount;
    private String status;
    private String bankAccountDocsId;
    private String reconciliationRunDocsId;
    private String statementDocumentDocsId;
    private Instant importedAt;
}
