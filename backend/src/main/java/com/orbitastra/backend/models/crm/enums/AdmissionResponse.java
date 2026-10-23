package com.orbitastra.backend.models.crm.enums;

/**
 * Explicit response submitted by the applicant to an AdmissionOffer. A pending
 * or expired offer has a null response rather than another enum value.
 */
public enum AdmissionResponse {
    /** Applicant accepted the offer. */
    ACCEPTED,

    /** Applicant declined the offer. */
    DECLINED
}
