package com.orbitastra.backend.models.crm.enums;

/**
 * Lifecycle of one revision of an AdmissionOffer.
 */
public enum AdmissionOfferStatus {
    /** Offer is being prepared and has not been sent. */
    DRAFT,

    /** Offer was formally sent to the applicant. */
    ISSUED,

    /** Applicant accepted the offer. */
    ACCEPTED,

    /** Applicant declined the offer. */
    DECLINED,

    /** Applicant did not respond before the deadline. */
    EXPIRED,

    /** School withdrew the offer. */
    WITHDRAWN,

    /** A newer revision replaced this offer. */
    SUPERSEDED
}
