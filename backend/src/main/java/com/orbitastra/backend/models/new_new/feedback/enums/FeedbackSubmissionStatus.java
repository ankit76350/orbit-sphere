package com.orbitastra.backend.models.new_new.feedback.enums;

/**
 * What has happened to one piece of feedback since it arrived.
 *
 * <p>Most feedback in a term-end drive never leaves SUBMITTED, and that is correct. Thirty
 * students rating a teacher four out of five is not thirty things for somebody to action.
 * These states are for the feedback that says something.
 *
 * <p>ACTIONED and DISMISSED are both endings, and keeping them apart is the point. DISMISSED
 * carries a reason, which means somebody had to write down why nothing was done. Feedback
 * that can be closed without a reason gets closed without a reason, and then a school can
 * say it reviewed everything while having acted on none of it.
 *
 * <p>ESCALATED is the state that says **this left the module.** Some feedback is not feedback
 * — it is an allegation that somebody was hurt. This package cannot handle that and should
 * not pretend to, so the state records that it went somewhere else and who took it. See the
 * README.
 *
 * <p>WITHDRAWN is only reachable for CONFIDENTIAL and IDENTIFIED feedback. Anonymous
 * feedback cannot be withdrawn, because nobody can prove it was theirs — which is a real
 * cost of true anonymity and worth knowing before choosing it.
 */
public enum FeedbackSubmissionStatus {
    /** Received. Nobody has looked at it yet. */
    SUBMITTED,

    /** Somebody is looking into it. */
    UNDER_REVIEW,

    /** Something was done, and what was done is recorded. */
    ACTIONED,

    /** Nothing was done, and the reason is recorded. */
    DISMISSED,

    /** Handed to a process outside this module. Who took it is recorded. */
    ESCALATED,

    /** Taken back by the submitter. Not available for anonymous feedback. */
    WITHDRAWN
}
