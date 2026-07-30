package com.orbitastra.backend.models.undone.a_new.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "payroll_adjustments")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_employment_effective_status_idx",
                def = "{'tenantId':1,'employmentRecordDocsId':1,'effectiveDate':1,'approvalState':1}"),
        @CompoundIndex(name = "tenant_external_adjustment_uniq",
                def = "{'tenantId':1,'sourceType':1,'externalReference':1}", unique = true,
                partialFilter = "{'externalReference':{'$type':'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAdjustment extends TenantScopedDocument {

    private String employmentRecordDocsId;
    private String payrollComponentDefinitionDocsId;
    private LocalDate effectiveDate;
    private String payPeriodKey;
    private BigDecimal amount;
    private String reason;
    private String sourceType;
    private String sourceDocsId;
    private String externalReference;
    private ApprovalState approvalState;
    private String approvedByDocsId;
    private String appliedPayrollResultDocsId;
}
