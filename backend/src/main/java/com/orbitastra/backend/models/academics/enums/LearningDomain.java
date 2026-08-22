package com.orbitastra.backend.models.academics.enums;

/**
 * An area of a child's development that the Holistic Progress Card reports on.
 *
 * <p>The first five are the domains the 2020 education policy names, and every card is
 * expected to cover them. The rest are areas schools commonly add, and a school that does
 * not use them simply leaves them out of its cards.
 *
 * <p>This is not a subject list. A subject is what is taught; a domain is what develops in
 * the child while it is taught. Language and Literacy is not the English period, and
 * Cognitive Development is not mathematics. Confusing the two turns the card back into a
 * report card with different words, which is the one thing it is meant not to be.
 *
 * <p>Finer detail belongs in the observation text on each domain, not in more enum values.
 * A school wanting to say something about a child's handwriting says it under Language and
 * Literacy rather than asking for a HANDWRITING domain.
 */
public enum LearningDomain {
    /** Body, movement, coordination, health and habits. A policy domain. */
    PHYSICAL_DEVELOPMENT,

    /** Feelings, relationships, fairness and self-awareness. A policy domain. */
    SOCIO_EMOTIONAL_ETHICAL,

    /** Thinking, reasoning, problem solving and curiosity. A policy domain. */
    COGNITIVE_DEVELOPMENT,

    /** Listening, speaking, reading and writing. A policy domain. */
    LANGUAGE_AND_LITERACY,

    /** Art, music, movement and appreciating them. A policy domain. */
    AESTHETIC_AND_CULTURAL,

    /** Using devices and information sensibly and safely. */
    DIGITAL_LITERACY,

    /** Leading, organising and taking responsibility for others. */
    LEADERSHIP_AND_INITIATIVE,

    /** Working with others, sharing and resolving disagreement. */
    COLLABORATION,

    /** Honesty, kindness, respect and civic sense. */
    VALUES_AND_CITIZENSHIP,

    /** Something the school reports on that the list above does not cover. */
    OTHER
}
