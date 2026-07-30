package com.orbitastra.backend.models.undone.a_new.billing;

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

@Document(collection = "stored_value_ledger_entries")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_account_sequence_uniq",
                def = "{'tenantId':1,'storedValueAccountDocsId':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_ledger_reference_uniq",
                def = "{'tenantId':1,'referenceType':1,'referenceDocsId':1,'entryType':1}", unique = true),
        @CompoundIndex(name = "tenant_account_occurred_idx",
                def = "{'tenantId':1,'storedValueAccountDocsId':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StoredValueLedgerEntry extends TenantScopedDocument {

    private String storedValueAccountDocsId;
    private Long sequenceNo;
    private String entryType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currencyCode;
    private String referenceType;
    private String referenceDocsId;
    private String idempotencyKey;
    private Instant occurredAt;
    private String postedByDocsId;
    private String reversalOfLedgerEntryDocsId;
    private String integrityHash;
}
