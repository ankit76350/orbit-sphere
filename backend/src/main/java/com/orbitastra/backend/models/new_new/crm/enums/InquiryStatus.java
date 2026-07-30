package com.orbitastra.backend.models.new_new.crm.enums;

/**
 * Current position of an Inquiry in the pre-application CRM pipeline.
 */
public enum InquiryStatus {
    /** Newly captured and not yet contacted. */
    NEW,

    /** The school has made the first successful contact. */
    CONTACTED,

    /** A counselor is actively discussing admission with the family. */
    COUNSELLING,

    /** A school visit has been scheduled. */
    VISIT_SCHEDULED,

    /** The prospective family completed the school visit. */
    VISITED,

    /** The family started a formal AdmissionApplication. */
    APPLICATION_STARTED,

    /** The linked AdmissionApplication was submitted. */
    APPLICATION_SUBMITTED,

    /** The prospect will not continue with admission. */
    LOST,

    /** CRM work is complete, normally after enrollment or administrative closure. */
    CLOSED
}
