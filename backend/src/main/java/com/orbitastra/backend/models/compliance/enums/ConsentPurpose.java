package com.orbitastra.backend.models.compliance.enums;

/**
 * What a family is being asked to agree to.
 *
 * <p>One consent per purpose, never one blanket agreement. A family that is happy for their
 * child's health details to be held by the school nurse may still refuse to have their photo
 * on the school's social media, and a single yes-or-no cannot hold both answers.
 *
 * <p>This is also what makes a withdrawal meaningful. Withdrawing consent for photographs
 * must not switch off the consent that lets the school keep medical records.
 */
public enum ConsentPurpose {
    /** Using the child's photograph or video outside the school. */
    PHOTOGRAPH_AND_MEDIA,

    /** Holding and using health and medical information. */
    HEALTH_DATA,

    /** Generating an APAAR number from the child's Aadhaar. */
    APAAR_GENERATION,

    /** Sharing academic results with a board, an authority or another school. */
    ACADEMIC_DATA_SHARING,

    /** Contacting the family by message, call or email. */
    COMMUNICATION,

    /** Using the school bus and holding the travel records that come with it. */
    TRANSPORT,

    /** Taking the child out of school on a trip. */
    TRIP_PARTICIPATION,

    /** Giving ordinary medicine such as paracetamol without ringing first. */
    ROUTINE_MEDICATION,

    /**
     * Giving one particular medicine or treatment, named on the consent. Not the standing
     * permission above: this is a course of antibiotics, or a single dose of something the
     * standing consent does not cover. Almost always RECORD_SPECIFIC.
     */
    MEDICAL_TREATMENT,

    /** The child living in the school hostel for a stated period. */
    HOSTEL_RESIDENCE,

    /**
     * Running a plan of extra learning support for the child. Some families decline, and a
     * plan running without their knowledge is how a school loses their trust.
     */
    LEARNING_SUPPORT,

    /** Anything the purposes above do not cover; the notes say what. */
    OTHER
}
