package com.orbitastra.backend.models.undone.a_new.payroll;

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

@Document(collection = "statutory_filings")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_legal_type_period_uniq",
                def = "{'tenantId':1,'legalEntityDocsId':1,'filingType':1,'periodKey':1}", unique = true),
        @CompoundIndex(name = "tenant_filing_status_due_idx",
                def = "{'tenantId':1,'status':1,'dueDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryFiling extends TenantScopedDocument {

    private String legalEntityDocsId;
    private String filingType;
    private String periodKey;
    private LocalDate dueDate;
    private String status;
    private String generatedDocumentDocsId;
    private String acknowledgementDocumentDocsId;
    private String authorityReference;
    private String submittedByDocsId;
    private Instant submittedAt;
}
