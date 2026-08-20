package com.orbitastra.backend.models.new_new.feedback.enums;

/**
 * Where a report sent to the head has got to.
 *
 * <p>ACKNOWLEDGED is the state this enum exists for, and leaving it out is the commonest way a
 * reporting channel dies. **A person who reports something and hears nothing back concludes
 * the channel does not work, and never uses it again** — and tells other people not to bother
 * either. One school year of silence is enough to make a speak-up channel worthless, and no
 * amount of policy repairs it afterwards.
 *
 * <p>So acknowledgement is a state with a clock on it, separate from anything being decided.
 * "We have read this and somebody is looking at it" is a different message from "here is what
 * we did", it arrives days earlier, and it is the one that keeps the channel alive.
 *
 * <p>AWAITING_REPORTER is the other state worth having. When the school asks a question — "which
 * class was this in?" — the report is not stalled by the school any more, and a queue that
 * cannot show that difference makes the office look slow for something it is waiting on.
 *
 * <p>ACTIONED and DISMISSED are both endings and both require the reason to be written down.
 * A report that can be closed with no reason gets closed with no reason.
 *
 * <p>ESCALATED means it left this module. An allegation that somebody was hurt is not something
 * this package handles, and the state records that it was handed to a person rather than
 * pretending a process happened here.
 */
public enum FeedbackReportStatus {
    /** Received. Nobody has confirmed reading it yet. The clock is running. */
    SUBMITTED,

    /** The recipient has confirmed they read it. The reporter has been told. */
    ACKNOWLEDGED,

    /** Being looked into. */
    UNDER_REVIEW,

    /** The school has asked the reporter something and is waiting for an answer. */
    AWAITING_REPORTER,

    /** Something was done, and what was done is recorded. */
    ACTIONED,

    /** Nothing was done, and the reason is recorded. */
    DISMISSED,

    /** Handed to a process outside this module. Who took it is recorded. */
    ESCALATED,

    /** Taken back by the reporter. */
    WITHDRAWN
}
