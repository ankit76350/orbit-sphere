package com.orbitastra.backend.models.new_new.crm;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A formal, versioned offer issued for an approved
 * {@link AdmissionApplication}.
 *
 * <p>{@code admissionApplicationDocsId} links to the application. Multiple
 * revisions are allowed, while the compound index prevents duplicate revision
 * numbers for the same application. Document, signature, deposit invoice, and
 * issuing-staff references remain external document ids.
 *
 * <p>Accepting an offer does not itself create a Student. The service must update
 * the offer and application consistently, then create and link the Student
 * during enrollment.
 */
@Document(collection = "admission_offers")
@CompoundIndexes({
                @CompoundIndex(name = "school_offer_no_uniq", def = "{'schoolId': 1, 'offerNo': 1}", unique = true),
                @CompoundIndex(name = "school_application_offer_revision_uniq", def = "{'schoolId': 1, 'admissionApplicationDocsId': 1, 'revisionNo': 1}", unique = true),
                @CompoundIndex(name = "school_offer_status_expiry_idx", def = "{'schoolId': 1, 'status': 1, 'expiresAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionOffer extends SchoolBase {

        // This stores the school’s formal admission offer after approval.

        // Example: "OFFER/2026/000001"
        @NotBlank
        private String offerNo;

        // Example: 1
        @NotNull
        @Builder.Default
        private Integer revisionNo = 1;

        // Links to AdmissionApplication.id. Example: "67aa15d9dc3f7d0077777777"
        @NotBlank
        private String admissionApplicationDocsId;

        // Links to the offered class/grade document. Example: "67aa15d9dc3f7d0033333333"
        @NotBlank
        private String offeredClassDocsId;

        // Example: AdmissionOfferStatus.DRAFT
        @NotNull
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

        // Links to the generated offer document. Example: "67aa15d9dc3f7d0099999991"
        private String offerDocumentDocsId;

        // Links to the stored acceptance signature. Example: "67aa15d9dc3f7d0099999992"
        private String acceptanceSignatureDocsId;

        // Links to the admission-deposit invoice. Example: "67aa15d9dc3f7d0099999993"
        private String depositInvoiceDocsId;

        // Links to the staff member who issued the offer. Example: "67aa15d9dc3f7d0088888888"
        private String issuedByDocsId;

        // Example: "Incorrect class was offered"
        private String withdrawalReason;
}
