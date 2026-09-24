package com.orbitastra.backend.dto.crm.admissionoffer.request;

import com.orbitastra.backend.models.crm.enums.AdmissionResponse;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #30 carries — the family's answer.
 *
 * <p><b>The answer is required and it is the whole point.</b> {@code ACCEPTED} or
 * {@code DECLINED}; there is no third value and no "maybe", because an offer a family has not
 * answered is simply one nobody has called this endpoint for.
 *
 * <p><b>It is the ANSWER, not the status.</b> {@code AdmissionResponse} has two values and
 * {@code AdmissionOfferStatus} has seven — the endpoint maps one to the other. That is different
 * from #20, which names a status directly: there the school is choosing among its own statuses,
 * here the family is choosing between yes and no, and the status that follows is the school's
 * bookkeeping rather than the family's decision.
 *
 * <p><b>{@code respondedAt} is not on the request.</b> It is stamped, like every other "when did
 * this happen" in the module — a caller-supplied timestamp is a caller who can say the family
 * answered before the offer was made.
 */
public record AdmissionOfferRespondRequest(

        @NotNull AdmissionResponse response,

        /**
         * A reference to the stored acceptance signature, for the schools that take one.
         *
         * <p><b>Not validated</b>, unlike #29's deposit invoice — it points at
         * {@code document_records}, which has no repository and no service either, and refusing
         * every value on a field a family's acceptance depends on would block the answer itself.
         * The deposit was different: it is optional to the act of offering.
         */
        @Size(max = 64) String acceptanceSignatureDocsId,

        Long version) {
}
