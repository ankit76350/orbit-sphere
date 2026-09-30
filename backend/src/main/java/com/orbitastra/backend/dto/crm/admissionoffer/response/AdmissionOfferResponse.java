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

        /**
         * Which letter this is for that family — 1 for the first, 2 for the one that replaced or
         * followed it. Example: 2
         *
         * <p><b>It was always 1 until 2026-09-30</b>, when the one-offer rule became a one-LIVE-
         * offer rule. A family who declined can be offered again, and a school re-offering on
         * different terms supersedes the old letter rather than editing it — so there can now be
         * several rows for one application, and this is what tells them apart.
         *
         * <p><b>The declared index still uses it</b>:
         * {@code school_application_offer_revision_uniq} is unique on
         * {@code (schoolId, admissionApplicationDocsId, revisionNo)}. Pinning the revision to 1
         * used to make that index mean "one offer per application"; counting it makes the same
         * index mean "one row per revision" — once it is built.
         */
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

        Instant createdAt,

        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static AdmissionOfferResponse fromOffer(AdmissionOffer offer, String applicationNo,
            String applicantName, String offeredClassName, String issuedByName, String nextStep) {

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
                offer.getCreatedAt(),
                offer.getVersion(),
                nextStep);
    }
}
