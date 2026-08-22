package com.orbitastra.backend.models.new_new.feedback.enums;

/**
 * What the school promised the person who gave the feedback.
 *
 * <p>This is the most important enum in the package, and the reason it is an enum at all is
 * that the earlier sketch had {@code Boolean anonymous} instead. A boolean cannot tell the
 * difference between the two promises below, and they are completely different promises.
 *
 * <p>ANONYMOUS means **the school does not know and cannot find out.** The submitter is not
 * stored. Nobody can be unmasked later, not by a head, not by an administrator with database
 * access, not by a court order. That is the whole point: a student will not say a teacher
 * shouts at them if there is any chance of it coming back.
 *
 * <p>CONFIDENTIAL means **the school knows but will not tell the subject.** The submitter is
 * stored encrypted and one narrow role can reveal it, and every reveal is written to the
 * audit trail. This is what a complaint needs, because somebody has to be able to ask a
 * follow-up question.
 *
 * <p>Most systems say "anonymous" and build CONFIDENTIAL, or worse: they store the submitter
 * in plain sight and hide the column on the screen. Then somebody exports to a spreadsheet
 * and a child's name is next to what they said about their teacher. **A person told
 * "anonymous" and later identified has been lied to**, and no amount of good intention at
 * the time repairs it. That is why these are two named values a service must choose between,
 * and why the choice has to be shown to the submitter before they type anything.
 */
public enum FeedbackAnonymityMode {
    /**
     * The submitter is not recorded at all. Unmaskable by anybody, ever, including the
     * school. No follow-up question can be asked.
     */
    ANONYMOUS,

    /**
     * The submitter is recorded encrypted. Hidden from the subject, revealable by one
     * narrow role, and every reveal is audited.
     */
    CONFIDENTIAL,

    /** The submitter is recorded normally and shown to whoever may read the feedback. */
    IDENTIFIED
}
