package com.orbitastra.backend.dto.crm.admissionoffer.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionResponse;

/**
 * One row of #32's chase list.
 *
 * <p><b>Thinner than the offer.</b> No {@code withdrawalReason}, no signature or invoice ids: the
 * reason is something a school wrote about one family and a page of twenty would carry all of it to
 * draw a list that shows none of it.
 *
 * <p><b>But the applicant IS named</b>, in one query for the whole page. A chase list of raw ids is
 * not a list anybody can work from — the point of it is to pick up the phone.
 */
public record AdmissionOfferSummaryResponse(

        String admissionOfferId,

        String offerNo,

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

        Instant createdAt,

        Long version) {

    public static AdmissionOfferSummaryResponse fromOffer(AdmissionOffer one,
            String applicationNo, String applicantName, String offeredClassName) {

        return new AdmissionOfferSummaryResponse(
                one.getId(),
                one.getOfferNo(),
                one.getAdmissionApplicationDocsId(),
                applicationNo,
                applicantName,
                one.getOfferedClassDocsId(),
                offeredClassName,
                one.getStatus(),
                one.getOfferedAt(),
                one.getExpiresAt(),
                one.getRespondedAt(),
                one.getResponse(),
                one.getCreatedAt(),
                one.getVersion());
    }
}
