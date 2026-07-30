package com.orbitastra.backend.models.undone.a_new.accounting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Immutable after posting. Corrections are modeled by reversal entries, never by
 * editing posted lines.
 */
@Document(collection = "journal_entries")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_legal_journal_no_uniq",
                def = "{'tenantId':1,'legalEntityDocsId':1,'journalNo':1}", unique = true),
        @CompoundIndex(name = "tenant_source_idempotency_uniq",
                def = "{'tenantId':1,'sourceType':1,'sourceDocsId':1,'idempotencyKey':1}",
                unique = true, partialFilter = "{'idempotencyKey':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_period_status_posted_idx",
                def = "{'tenantId':1,'fiscalPeriodDocsId':1,'status':1,'postedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry extends TenantScopedDocument {

    public enum JournalStatus {
        DRAFT,
        PENDING_APPROVAL,
        POSTED,
        REVERSED,
        REJECTED
    }

    private String legalEntityDocsId;
    private String fiscalPeriodDocsId;
    private String journalNo;
    private LocalDate accountingDate;
    private String description;
    private String currencyCode;
    private JournalStatus status;
    private String sourceType;
    private String sourceDocsId;
    private String idempotencyKey;
    private String approvedByDocsId;
    private String postedByDocsId;
    private Instant postedAt;
    private String reversalOfJournalDocsId;
    private String integrityHash;

    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLine {
        private Integer lineNo;
        private String ledgerAccountDocsId;
        private String campusDocsId;
        private String costCentreDocsId;
        private String partyType;
        private String partyDocsId;
        private BigDecimal debit;
        private BigDecimal credit;
        private String memo;
    }
}
