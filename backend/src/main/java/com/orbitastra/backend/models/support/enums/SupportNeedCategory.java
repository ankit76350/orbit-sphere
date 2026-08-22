package com.orbitastra.backend.models.support.enums;

/**
 * The kind of extra help a child needs with learning.
 *
 * <p>Broad on purpose. A precise clinical name belongs in the assessment report attached to
 * the need, not in an enum: a school is not diagnosing anybody, it is recording what a
 * specialist said and deciding what to do in the classroom.
 *
 * <p>ACADEMIC_CATCH_UP is here alongside the rest because most children who need extra help do
 * not have a diagnosis at all. They missed a year, changed language of instruction, or fell
 * behind in one subject. A module that only handled diagnosed conditions would miss the larger
 * group, and those children need a plan just as much.
 */
public enum SupportNeedCategory {
    /** Dyslexia, dyscalculia, dysgraphia and similar. */
    SPECIFIC_LEARNING_DIFFICULTY,

    /** Does not hear well, with or without an aid. */
    HEARING,

    /** Does not see well, with or without glasses. */
    VISION,

    /** Difficulty speaking or understanding spoken language. */
    SPEECH_AND_LANGUAGE,

    /** Difficulty moving about the school or using their hands. */
    PHYSICAL_MOBILITY,

    /** Difficulty holding attention or sitting still. */
    ATTENTION,

    /** On the autism spectrum. */
    AUTISM_SPECTRUM,

    /** Behind in one or more subjects, with no diagnosis. The largest group. */
    ACADEMIC_CATCH_UP,

    /** New to the language the school teaches in. */
    LANGUAGE_OF_INSTRUCTION,

    /** Ahead of the class and needing more, which is also a support need. */
    GIFTED_AND_TALENTED,

    /** Something the list above does not cover; the notes say what. */
    OTHER
}
