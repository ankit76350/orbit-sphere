package com.orbitastra.backend.models.new_new.crm.enums;

/**
 * Current state of a formal AdmissionApplication.
 */
public enum AdmissionApplicationStatus {
    /** Applicant is still preparing the form. */
    DRAFT,

    /** Applicant submitted the form to the school. */
    SUBMITTED,

    /** One or more admission reviews are in progress. */
    UNDER_REVIEW,

    /** The school requested missing or corrected information. */
    ADDITIONAL_INFORMATION_REQUIRED,

    /** Applicant is waiting for an available seat. */
    WAITLISTED,

    /** Application was approved before an offer was issued. */
    APPROVED,

    /** Application was rejected. */
    REJECTED,

    /** Applicant withdrew the application. */
    WITHDRAWN,

    /** At least one current admission offer has been issued. */
    OFFERED,

    /** The applicant accepted the current admission offer. */
    OFFER_ACCEPTED,

    /** A Student was created and linked through resultingStudentDocsId. */
    ENROLLED
}
