package com.orbitastra.backend.models.undone.a_new.payroll;

import java.math.BigDecimal;
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

@Document(collection = "payroll_results")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_run_employment_uniq",
                def = "{'tenantId':1,'payrollRunDocsId':1,'employmentRecordDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_employment_pay_period_idx",
                def = "{'tenantId':1,'employmentRecordDocsId':1,'payPeriodKey':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResult extends TenantScopedDocument {

    private String payrollRunDocsId;
    private String employmentRecordDocsId;
    private String personDocsId;
    private String compensationPlanDocsId;
    private String payPeriodKey;
    private String currencyCode;
    private BigDecimal grossAmount;
    private BigDecimal deductionAmount;
    private BigDecimal employerContributionAmount;
    private BigDecimal netAmount;
    private String status;
    private String calculationTraceHash;
    private String payslipDocumentDocsId;

    @Builder.Default
    private List<ResultLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultLine {
        private Integer lineNo;
        private String componentCodeSnapshot;
        private String componentNameSnapshot;
        private String componentType;
        private BigDecimal quantity;
        private BigDecimal rate;
        private BigDecimal amount;
        private String calculationTrace;
    }
}
