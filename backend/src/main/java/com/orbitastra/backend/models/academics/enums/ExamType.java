package com.orbitastra.backend.models.new_new.academics.enums;

/**
 * Nature of an examination event, used for grouping, weighting semantics, and
 * result reporting. The school-specific name stays in {@code Exam.name}.
 */
public enum ExamType {
    /** Short periodic classroom test. */
    UNIT_TEST,

    /** Continuous or formative assessment that tracks progress. */
    FORMATIVE,

    /** Summative assessment covering a completed portion of the syllabus. */
    SUMMATIVE,

    /** Mid-term examination. */
    MIDTERM,

    /** Final examination of a term or academic year. */
    FINAL,

    /** Practical, laboratory, or performance examination. */
    PRACTICAL,

    /** School examination held in preparation for a board examination. */
    PRE_BOARD,

    /** Examination conducted or governed by an external education board. */
    BOARD,

    /** School-defined type not represented above. */
    OTHER
}
