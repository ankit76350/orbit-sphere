package com.orbitastra.backend.models.new_new.compliance.enums;

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

    /** Anything the purposes above do not cover; the notes say what. */
    OTHER
}
