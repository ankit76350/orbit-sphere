package com.orbitastra.backend.models.undone.a_new.accounting;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "reconciliation_runs")
@CompoundIndex(name = "tenant_account_period_run_uniq",
        def = "{'tenantId':1,'bankAccountDocsId':1,'statementPeriodKey':1,'runNo':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun extends TenantScopedDocument {

    private String bankAccountDocsId;
    private String statementDocumentDocsId;
    private String statementPeriodKey;
    private Integer runNo;
    private ApprovalState state;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private Integer matchedCount;
    private Integer unmatchedCount;
    private BigDecimal unmatchedAmount;
    private Instant completedAt;
    private String completedByDocsId;
}
