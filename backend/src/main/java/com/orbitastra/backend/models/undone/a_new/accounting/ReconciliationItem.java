package com.orbitastra.backend.models.undone.a_new.accounting;

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

@Document(collection = "reconciliation_items")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_run_statement_line_uniq",
                def = "{'tenantId':1,'reconciliationRunDocsId':1,'statementLineKey':1}", unique = true),
        @CompoundIndex(name = "tenant_run_match_state_idx",
                def = "{'tenantId':1,'reconciliationRunDocsId':1,'matchState':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItem extends TenantScopedDocument {

    public enum MatchState {
        UNMATCHED,
        SUGGESTED,
        MATCHED,
        IGNORED
    }

    private String reconciliationRunDocsId;
    private String statementLineKey;
    private LocalDate valueDate;
    private String description;
    private BigDecimal amount;
    private MatchState matchState;
    private String journalEntryDocsId;
    private String paymentDocsId;
    private String matchedByDocsId;
    private String matchingRuleKey;
    private Double confidenceScore;
}
