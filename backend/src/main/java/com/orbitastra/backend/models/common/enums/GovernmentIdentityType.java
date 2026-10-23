package com.orbitastra.backend.models.common.enums;

/**
 * What kind of government or tax document somebody showed.
 *
 * <p>Used for staff, whose identity the school keeps on file, and for visitors,
 * who show something at the gate. It sits in common rather than under people
 * because both need it and neither owns it.
 */
public enum GovernmentIdentityType {
    AADHAAR,
    PAN,
    PASSPORT,
    NATIONAL_ID,
    TAX_ID,
    DRIVING_LICENCE,
    VOTER_ID,

    /**
     * Automated Permanent Academic Account Registry number. A lifelong academic id for a
     * student, generated from their Aadhaar under the 2020 education policy.
     */
    APAAR,

    /** Permanent Education Number, issued by a state to a student. */
    PEN,

    OTHER
}
