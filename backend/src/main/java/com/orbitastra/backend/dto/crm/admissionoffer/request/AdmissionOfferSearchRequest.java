package com.orbitastra.backend.dto.crm.admissionoffer.request;

import java.time.Instant;

import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;

/**
 * What #32 filters by — <b>the chase list</b>.
 *
 * <p><b>{@code status} and {@code expiringBefore} are the two the index is built for</b>, in its
 * order: {@code school_offer_status_expiry_idx} is
 * {@code {schoolId, status, expiresAt}}, which is this endpoint's whole reason for existing.
 *
 * <p><b>{@code expired} is the sharper question</b> and the one worth asking: past its date
 * <i>and</i> still {@code ISSUED}. A date in the past is not enough — an offer a family accepted
 * last month has one too, and nobody needs chasing about it.
 */
public record AdmissionOfferSearchRequest(

        AdmissionOfferStatus status,

        String admissionApplicationDocsId,

        String offeredClassDocsId,

        /** Everything lapsing before this instant. The "who do I ring this week" filter. */
        Instant expiringBefore,

        /** Past its date and still outstanding. Not simply "has a past date". */
        Boolean expired,

        Integer page,
        Integer size,
        String sort) {
}
