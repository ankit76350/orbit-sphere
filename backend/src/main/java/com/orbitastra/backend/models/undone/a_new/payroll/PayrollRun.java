package com.orbitastra.backend.models.undone.a_new.payroll;

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

@Document(collection = "payroll_runs_v2")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_legal_pay_period_run_uniq",
                def = "{'tenantId':1,'legalEntityDocsId':1,'payPeriodKey':1,'runType':1}", unique = true),
        @CompoundIndex(name = "tenant_payroll_status_period_idx",
                def = "{'tenantId':1,'status':1,'periodEnd':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRun extends TenantScopedDocument {

    private String legalEntityDocsId;
    private String payPeriodKey;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String runType;
    private String status;
    private String currencyCode;
    private Integer employeeCount;
    private BigDecimal grossTotal;
    private BigDecimal deductionTotal;
    private BigDecimal employerContributionTotal;
    private BigDecimal netTotal;
    private String inputSnapshotHash;
    private String idempotencyKey;
    private String approvedByDocsId;
    private String postedJournalEntryDocsId;
    private Instant calculatedAt;
    private Instant approvedAt;
    private Instant postedAt;
}
