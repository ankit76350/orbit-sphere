package com.orbitastra.backend.models.new_new.feedback.enums;

/**
 * How one question on a feedback form is answered.
 *
 * <p>The type decides which field on FeedbackAnswer is filled in, and whether the answer can
 * be averaged. RATING and YES_NO produce numbers a report can add up. TEXT does not, and a
 * form made only of TEXT questions gives a school a pile of reading and no way to tell
 * whether anything improved between last term and this one.
 *
 * <p>SINGLE_CHOICE and MULTI_CHOICE are counted rather than averaged. "Which of these did
 * you find difficult" has no meaningful mean.
 */
public enum FeedbackQuestionType {
    /** A number on a scale, one up to the topic's maximum. Averages meaningfully. */
    RATING,

    /** Yes or no. Counted as a percentage answering yes. */
    YES_NO,

    /** One option from a fixed list. Counted, not averaged. */
    SINGLE_CHOICE,

    /** Any number of options from a fixed list. Counted, not averaged. */
    MULTI_CHOICE,

    /** Free text. Read, never averaged. */
    TEXT
}
