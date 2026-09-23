package com.orbitastra.backend.dto.crm.admissionapplication.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #22 carries — who owns this application from here on.
 *
 * <p><b>The officer is required.</b> This endpoint hands a form to somebody; a body without a
 * person in it is asking for nothing, and there is no "unassign" in the plan. A school whose
 * officer leaves gives the form to somebody else, which is the same call with a different id.
 *
 * <p><b>It is the only field, and that is the whole shape of the endpoint.</b> Assigning an owner
 * is not a decision: it moves no status, stamps no date and writes nothing else on the form.
 *
 * <p><b>{@code version} is optional.</b> Send what you read for
 * {@code 409 CONCURRENT_MODIFICATION} when somebody reassigned the form while you were looking;
 * leave it out and the last write wins.
 */
public record AdmissionApplicationAssignRequest(

        @NotBlank @Size(max = 64) String assignedAdmissionOfficerDocsId,

        Long version) {
}
