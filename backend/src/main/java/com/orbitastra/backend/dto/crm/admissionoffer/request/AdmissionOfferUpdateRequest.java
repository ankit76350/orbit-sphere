package com.orbitastra.backend.dto.crm.admissionoffer.request;

import java.time.Instant;

import jakarta.validation.constraints.Size;

/**
 * What #29b carries — a correction to the one offer letter this admission has.
 *
 * <p><b>This endpoint exists because the one-offer rule opened a hole.</b> A school issues one
 * letter per admission; if it lapses, nothing could extend it and nothing could issue another, so a
 * family that missed the deadline could not be given a seat by any route. That was a dead end in
 * something already shipped rather than a feature nobody had built yet.
 *
 * <p><b>Every field is optional and a body carrying none is {@code 400 NOTHING_TO_UPDATE}</b>, the
 * same shape #27 has. A no-op that answered 200 could not be told apart from a change that worked.
 *
 * <p><b>{@code expiresAt} is the one it is for.</b> Extending a lapsed offer is the whole point —
 * and it works because nothing writes {@code EXPIRED}: a lapsed offer is still stored as
 * {@code ISSUED}, so it is still an offer this can reach.
 *
 * <p><b>There is no way to CLEAR the expiry.</b> An {@code Instant} has no empty form the way a
 * String does, and "a seat held for ever" is not a correction anybody means to make — the same call
 * the cycle's four dates make, which can be moved but not emptied.
 *
 * <p><b>{@code status} is not here, and neither is {@code response}.</b> Those are #30's and #31's,
 * and an edit that could set them would be a second way to answer for a family.
 */
public record AdmissionOfferUpdateRequest(

        /** Extend it, or bring it forward. Never into the past. */
        Instant expiresAt,

        /**
         * A different grade, when the school corrects what it offered.
         *
         * <p>Checked exactly as #29 checks it: a class of the <b>cycle's</b> year that the round
         * has seats set up for.
         */
        @Size(max = 64) String offeredClassDocsId,

        /** Checked against {@code fee_invoices}, exactly as #29 checks it. */
        @Size(max = 64) String depositInvoiceDocsId,

        Long version) {
}
