package com.orbitastra.backend.dto.crm.admissionoffer.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #31 carries — why the school is taking the offer back.
 *
 * <p><b>The reason is the whole body and it is required.</b> A seat promised to a family and then
 * taken away is the part of an admissions record worth the most, and the same reading makes
 * {@code lostReason} required on a lost inquiry and a note required on a refused application.
 *
 * <p><b>{@code @NotBlank} here, unlike #27d's note.</b> That one could fall back on notes the
 * reviewer had already written; there is nothing on an offer that could stand in for why it was
 * withdrawn, so the request has to carry it.
 */
public record AdmissionOfferWithdrawRequest(

        @NotBlank @Size(max = 2000) String withdrawalReason,

        Long version) {
}
