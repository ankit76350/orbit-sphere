package com.orbitastra.backend.models.undone.a_new.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "compensation_plans")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_employment_effective_uniq",
                def = "{'tenantId':1,'employmentRecordDocsId':1,'effectiveFrom':1}", unique = true),
        @CompoundIndex(name = "tenant_employment_active_period_idx",
                def = "{'tenantId':1,'employmentRecordDocsId':1,'effectiveFrom':-1,'effectiveTo':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationPlan extends TenantScopedDocument {

    private String employmentRecordDocsId;
    private String currencyCode;
    private String payFrequency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal annualCostToCompany;
    private ApprovalState approvalState;
    private String approvedByDocsId;
    private String revisionReason;
    private String supersedesCompensationPlanDocsId;

    @Builder.Default
    private List<ComponentValue> componentValues = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentValue {
        private String payrollComponentDefinitionDocsId;
        private BigDecimal amount;
        private BigDecimal rate;
        private String calculationOverride;
    }
}
