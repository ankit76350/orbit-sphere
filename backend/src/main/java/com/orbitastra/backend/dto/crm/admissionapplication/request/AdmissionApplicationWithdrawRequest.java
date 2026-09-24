package com.orbitastra.backend.dto.crm.admissionapplication.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #21 carries — why the family pulled out.
 *
 * <p><b>The reason is the whole body and it is required.</b> A family walking away is the part of
 * an admissions record a school learns most from — they went to another school, the fees were too
 * high, they moved city — and a withdrawal with nothing said teaches nobody anything. The same
 * reading makes {@code lostReason} required on a lost inquiry and a note required on a refused
 * application.
 *
 * <p><b>It is the FAMILY'S reason, not the school's.</b> #20's note is what the school decided and
 * why; this is what the family told them. They are different facts and they live in different
 * fields, which is why withdrawing does not write {@code decisionNote}.
 *
 * <p><b>{@code withdrawnAt} is not on the request</b>, like every other "when did this happen" in
 * the module. A caller-supplied timestamp is a caller who can say a family pulled out before they
 * applied.
 */
public record AdmissionApplicationWithdrawRequest(

        @NotBlank @Size(max = 2000) String withdrawalReason,

        Long version) {
}
