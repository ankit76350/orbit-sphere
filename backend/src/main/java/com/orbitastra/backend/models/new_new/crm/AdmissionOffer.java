package com.orbitastra.backend.models.new_new.crm;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "admission_offers")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_offer_no_uniq",
                def = "{'schoolId': 1, 'offerNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_application_offer_revision_uniq",
                def = "{'schoolId': 1, 'admissionApplicationDocsId': 1, 'revisionNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_offer_status_expiry_idx",
                def = "{'schoolId': 1, 'status': 1, 'expiresAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionOffer extends SchoolBase {

    // Example: "OFFER/2026/000001"
    private String offerNo;

    // Example: 1
    @Builder.Default
    private Integer revisionNo = 1;

    // Example: "67aa15d9dc3f7d0077777777"
    private String admissionApplicationDocsId;

    // Example: "67aa15d9dc3f7d0033333333"
    private String offeredClassDocsId;

    // Example: AdmissionOfferStatus.DRAFT
    @Builder.Default
    private AdmissionOfferStatus status = AdmissionOfferStatus.DRAFT;

    // Example: 2026-03-20T10:00:00Z
    private Instant offeredAt;

    // Example: 2026-03-31T23:59:59Z
    private Instant expiresAt;

    // Example: 2026-03-22T08:30:00Z
    private Instant respondedAt;

    // Example: AdmissionResponse.ACCEPTED
    private AdmissionResponse response;

    // Example: "67aa15d9dc3f7d0099999991"
    private String offerDocumentDocsId;

    // Example: "67aa15d9dc3f7d0099999992"
    private String acceptanceSignatureDocsId;

    // Example: "67aa15d9dc3f7d0099999993"
    private String depositInvoiceDocsId;

    // Example: "67aa15d9dc3f7d0088888888"
    private String issuedByDocsId;

    // Example: "Incorrect class was offered"
    private String withdrawalReason;
}
