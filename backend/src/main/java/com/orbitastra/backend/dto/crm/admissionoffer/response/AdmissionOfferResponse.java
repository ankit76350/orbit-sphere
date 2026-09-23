package com.orbitastra.backend.dto.crm.admissionoffer.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionResponse;

/**
 * One offer, in full.
 *
 * <p><b>The same fields #25 nests under {@code offers}</b>, plus the three a row of that list does
 * not need: the application it is for, the person who issued it, and what to do next. A caller who
 * has just issued one is holding the answer; a caller reading the form back is looking at a list.
 *
 * <p><b>The names are resolved, not just the ids.</b> The class was read to refuse one the cycle has
 * no seats for, and the staff member to refuse an id that is not this school's — so both names are
 * already in hand and cost no second query.
 */
public record AdmissionOfferResponse(

        String admissionOfferId,

        String offerNo,

        Integer revisionNo,

        String admissionApplicationDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String applicationNo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String applicantName,

        String offeredClassDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String offeredClassName,

        AdmissionOfferStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant offeredAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant expiresAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant respondedAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        AdmissionResponse response,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String depositInvoiceDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String issuedByDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String issuedByName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String withdrawalReason,

        /**
         * How many revisions this application now has, this one included.
         *
         * <p><b>It is what makes superseding visible in the answer.</b> A caller who issues a
         * second offer gets {@code revisionNo: 2} and {@code revisionCount: 2} — and knows without
         * a second read that something was superseded rather than that they created the first one.
         */
        Integer revisionCount,

        Instant createdAt,

        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static AdmissionOfferResponse fromOffer(AdmissionOffer offer, String applicationNo,
            String applicantName, String offeredClassName, String issuedByName,
            Integer revisionCount, String nextStep) {

        return new AdmissionOfferResponse(
                offer.getId(),
                offer.getOfferNo(),
                offer.getRevisionNo(),
                offer.getAdmissionApplicationDocsId(),
                applicationNo,
                applicantName,
                offer.getOfferedClassDocsId(),
                offeredClassName,
                offer.getStatus(),
                offer.getOfferedAt(),
                offer.getExpiresAt(),
                offer.getRespondedAt(),
                offer.getResponse(),
                offer.getDepositInvoiceDocsId(),
                offer.getIssuedByDocsId(),
                issuedByName,
                offer.getWithdrawalReason(),
                revisionCount,
                offer.getCreatedAt(),
                offer.getVersion(),
                nextStep);
    }
}
