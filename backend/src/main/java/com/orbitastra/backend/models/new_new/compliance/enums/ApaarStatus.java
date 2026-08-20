package com.orbitastra.backend.models.new_new.compliance.enums;

/**
 * How far a student's APAAR number has got.
 *
 * <p>APAAR is the lifelong academic id introduced under the 2020 education policy. It is
 * generated from a student's Aadhaar with the family's consent, and schools are asked to
 * report how many of their students have one.
 *
 * <p>NOT_APPLIED and ERROR are the two that matter for that report. A school needs the list
 * of children nobody has applied for and the list where the application came back rejected,
 * and those are different problems: one needs somebody to do the work, the other needs
 * somebody to find out why the details did not match.
 */
public enum ApaarStatus {
    /** Nobody has applied for this student yet. */
    NOT_APPLIED,

    /** The family has not agreed to it, so it cannot be applied for. */
    CONSENT_PENDING,

    /** Applied for and waiting on the registry. */
    PENDING,

    /** Issued. The number is on the record. */
    GENERATED,

    /** The application was refused, usually because details did not match Aadhaar. */
    ERROR
}
