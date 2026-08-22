package com.orbitastra.backend.models.finance.enums;

/**
 * Who handed over the money for a FeePayment.
 *
 * <p>The student the invoice belongs to is always stored separately, so a
 * payment made by a sponsor is still linked to the right student.
 */
public enum PayerType {
    /** A parent or another guardian of the student. */
    GUARDIAN,

    /** The student themself. */
    STUDENT,

    /** An outside company or person paying for the student. */
    SPONSOR,

    /** A scholarship or aid programme paying on the student's behalf. */
    AID_PROGRAMME,

    /** A government scheme paying on the student's behalf. */
    GOVERNMENT,

    /** The school itself, used when a balance is adjusted internally. */
    SCHOOL,

    /** Anyone the types above do not cover. */
    OTHER
}
