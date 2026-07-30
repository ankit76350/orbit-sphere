package com.orbitastra.backend.models.undone.a_new.accounting;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "fiscal_periods")
@CompoundIndex(name = "tenant_legal_fiscal_period_uniq",
        def = "{'tenantId':1,'legalEntityDocsId':1,'periodCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalPeriod extends TenantScopedDocument {

    public enum PeriodStatus {
        FUTURE,
        OPEN,
        SOFT_CLOSED,
        CLOSED,
        REOPENED
    }

    private String legalEntityDocsId;
    private String periodCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodStatus status;
    private String closedByDocsId;
    private String closeRunDocsId;
}
