package com.orbitastra.backend.dto.crm.admissionreview.request;

import jakarta.validation.constraints.Size;

/**
 * What #27d carries — the school called the review off, and this is why.
 *
 * <p><b>The reason is the whole body.</b> Work abandoned with no reason is a gap in the record, the
 * same reading that makes {@code lostReason} required on a lost inquiry and a note required on a
 * refused application.
 *
 * <p><b>It is written to {@code notes}, and it is called {@code notes} here too.</b> There is one
 * field on the document and giving it a second name in this DTO would mean a caller reading the
 * review back finds their {@code reason} under another word.
 *
 * <p><b>Not {@code @NotBlank}</b>, for the same reason {@code recommendation} is not
 * {@code @NotNull} on #27c: a review that already carries notes has said why, and the check belongs
 * where the review is in hand — as {@code 400 CANCELLATION_NOTE_REQUIRED}, the code #27 already
 * uses for this.
 */
public record AdmissionReviewCancelRequest(

        @Size(max = 2000) String notes,

        Long version) {
}
