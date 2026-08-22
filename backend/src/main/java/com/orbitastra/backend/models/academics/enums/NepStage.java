package com.orbitastra.backend.models.new_new.academics.enums;

/**
 * Which stage of schooling a child is in, under the 2020 education policy's five plus three
 * plus three plus four structure.
 *
 * <p>It matters here because a Holistic Progress Card does not look the same at every stage.
 * A card for a five-year-old is mostly the teacher's observation and a parent's note; a card
 * for a fifteen-year-old carries the child's own reflection and sits beside real
 * examination results. Recording the stage means a card can be read years later knowing
 * which kind it was.
 *
 * <p>The stage is not the class. It is worked out from the class, and it is stored on the
 * card because a class is renamed and reorganised over the years while the stage a child was
 * in does not change after the fact.
 */
public enum NepStage {
    /** Ages three to eight, roughly pre-school to Class II. */
    FOUNDATIONAL,

    /** Ages eight to eleven, roughly Classes III to V. */
    PREPARATORY,

    /** Ages eleven to fourteen, roughly Classes VI to VIII. */
    MIDDLE,

    /** Ages fourteen to eighteen, roughly Classes IX to XII. */
    SECONDARY
}
