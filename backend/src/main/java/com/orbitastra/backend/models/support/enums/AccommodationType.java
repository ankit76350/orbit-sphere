package com.orbitastra.backend.models.new_new.support.enums;

/**
 * One adjustment the school makes for a child.
 *
 * <p>These are the concrete things that actually change a child's day, and the reason this
 * module is worth building. A plan full of good intentions changes nothing; "twenty-five percent
 * extra time, a separate room, and a reader for the question paper" changes an exam.
 *
 * <p>The exam ones are the sharpest, because an accommodation that does not reach the
 * invigilator on the day did not happen. See the README.
 */
public enum AccommodationType {
    /** More time to finish, as a percentage of the normal duration. */
    EXTRA_TIME,

    /** Somebody writes down what the child dictates. */
    SCRIBE,

    /** Somebody reads the question paper aloud. */
    READER,

    /** Question paper in a larger font. */
    LARGE_PRINT,

    /** Sits at the front, near the teacher, or away from a window. */
    PREFERENTIAL_SEATING,

    /** Sits the exam in a separate quiet room. */
    SEPARATE_ROOM,

    /** Rest breaks during a lesson or a paper. */
    SUPERVISED_BREAKS,

    /** Uses a hearing aid, a magnifier, a laptop or similar. */
    ASSISTIVE_DEVICE,

    /** Allowed a calculator where others are not. */
    CALCULATOR_ALLOWED,

    /** Answers aloud instead of writing. */
    ORAL_ASSESSMENT,

    /** Fewer or different questions, or a reduced syllabus. */
    MODIFIED_PAPER,

    /** Extra classes outside normal teaching time. */
    REMEDIAL_TEACHING,

    /** Excused from a subject or an activity altogether. */
    EXEMPTION,

    /** Something the list above does not cover; the description says what. */
    OTHER
}
