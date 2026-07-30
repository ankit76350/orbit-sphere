package com.orbitastra.backend.models.undone.a_new.governance;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "governing_bodies")
@CompoundIndex(name = "tenant_governing_body_code_uniq",
        def = "{'tenantId':1,'bodyCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GoverningBody extends TenantScopedDocument {

    public enum BodyType {
        BOARD,
        TRUST,
        SCHOOL_MANAGING_COMMITTEE,
        ACADEMIC_COUNCIL,
        FINANCE_COMMITTEE,
        SAFEGUARDING_COMMITTEE,
        PROCUREMENT_COMMITTEE,
        OTHER
    }

    private String bodyCode;
    private String name;
    private BodyType type;
    private String legalEntityDocsId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private String termsOfReferenceDocumentDocsId;
    private Integer quorum;
}
