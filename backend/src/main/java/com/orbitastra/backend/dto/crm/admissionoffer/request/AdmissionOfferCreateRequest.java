package com.orbitastra.backend.dto.crm.admissionoffer.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #29 carries — the seat the school is offering, and until when.
 *
 * <p><b>There is no status field.</b> Issuing is the endpoint, so the offer is created
 * {@code ISSUED} and {@code offeredAt} is stamped. A body naming a status would be a second way to
 * say the same thing and a way to disagree with the path — the call this module makes on #19, #3,
 * #22 and the three review verbs.
 *
 * <p><b>And no {@code revisionNo}.</b> It is {@code max + 1} for the application, worked out from
 * what is already stored. A caller-supplied revision is a caller who can overwrite the history of
 * what was offered, which is the one thing keeping every revision is for.
 *
 * <p><b>{@code offeredClassDocsId} is required and is NOT always the applied class.</b> A school
 * assesses a child and offers a different grade; that is the whole reason the offer carries a class
 * of its own rather than reading the application's.
 *
 * <p><b>{@code expiresAt} is optional and defaults to the cycle's {@code enrollmentDeadlineAt}</b> —
 * the date the school already published for that round. An offer with no deadline at all is a seat
 * held for ever, so the default matters more than the field.
 */
public record AdmissionOfferCreateRequest(

        @NotBlank @Size(max = 64) String offeredClassDocsId,

        Instant expiresAt,

        @Size(max = 64) String depositInvoiceDocsId,

        @Size(max = 64) String issuedByDocsId) {
}
