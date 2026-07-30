package com.orbitastra.backend.models.undone.a_new.payroll;

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

@Document(collection = "payroll_component_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_payroll_component_code_uniq",
                def = "{'tenantId':1,'code':1}", unique = true),
        @CompoundIndex(name = "tenant_payroll_component_type_active_idx",
                def = "{'tenantId':1,'componentType':1,'active':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollComponentDefinition extends TenantScopedDocument {

    public enum ComponentType {
        EARNING,
        DEDUCTION,
        EMPLOYER_CONTRIBUTION,
        REIMBURSEMENT,
        TAX
    }

    private String code;
    private String name;
    private ComponentType componentType;
    private String calculationMethod;
    private String formulaExpression;
    private BigDecimal defaultAmount;
    private BigDecimal defaultRate;
    private Boolean taxable;
    private Boolean statutory;
    private String statutoryCode;
    private String expenseLedgerAccountDocsId;
    private String liabilityLedgerAccountDocsId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
}
