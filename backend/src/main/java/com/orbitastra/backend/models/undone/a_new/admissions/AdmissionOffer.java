package com.orbitastra.backend.models.undone.a_new.admissions;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "admission_offers")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_offer_no_uniq",
                def = "{'tenantId':1,'offerNo':1}", unique = true),
        @CompoundIndex(name = "tenant_application_offer_revision_uniq",
                def = "{'tenantId':1,'admissionApplicationDocsId':1,'revisionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_offer_status_expiry_idx",
                def = "{'tenantId':1,'status':1,'expiresAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionOffer extends AcademicScopedDocument {

    private String offerNo;
    private Integer revisionNo;
    private String admissionApplicationDocsId;
    private String offeredGradeNodeDocsId;
    private String status;
    private Instant offeredAt;
    private Instant expiresAt;
    private Instant respondedAt;
    private String response;
    private String offerDocumentDocsId;
    private String acceptanceSignatureDocsId;
    private String depositInvoiceDocsId;
    private String enrollmentDocsId;
}
