package com.orbitastra.backend.models.undone.a_new.accounting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "budget_plans")
@CompoundIndex(name = "tenant_fiscal_budget_version_uniq",
        def = "{'tenantId':1,'fiscalYearKey':1,'budgetCode':1,'budgetVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPlan extends TenantScopedDocument {

    private String fiscalYearKey;
    private String budgetCode;
    private Integer budgetVersion;
    private String name;
    private String currencyCode;
    private ApprovalState state;
    private String approvedByDocsId;

    @Builder.Default
    private List<BudgetLine> lines = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetLine {
        private String ledgerAccountDocsId;
        private String campusDocsId;
        private String costCentreDocsId;
        private Integer periodNo;
        private BigDecimal amount;
    }
}
