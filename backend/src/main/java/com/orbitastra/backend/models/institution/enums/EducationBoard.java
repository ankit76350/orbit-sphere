package com.orbitastra.backend.models.institution.enums;

/**
 * Standard board family used by an AffiliationProgramme. Board-specific names
 * remain in AffiliationProgramme.boardName.
 */
public enum EducationBoard {
    /** Central Board of Secondary Education, India. */
    CBSE,

    /** Council for the Indian School Certificate Examinations. */
    CISCE,

    /** Indian state or union-territory education board. */
    STATE_BOARD,

    /** International Baccalaureate. */
    IB,

    /** Cambridge International Education. */
    CAMBRIDGE,

    /** Another national education board outside the predefined Indian boards. */
    NATIONAL,

    /** Board family not represented above. */
    OTHER
}
