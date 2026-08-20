package com.orbitastra.backend.models.new_new.feedback.campaign.enums;

/**
 * Where a feedback drive has got to.
 *
 * <p>CLOSED and PUBLISHED are two states because they are two decisions. Closing stops new
 * submissions. Publishing releases the results to the people they are about. A school will
 * often want a week between the two, so that a head can read what came in before a teacher
 * does — and so that a set of results with four responses can be held back rather than
 * released because the calendar said so.
 *
 * <p>One state for both would mean the moment the last student submits, every teacher can
 * read their comments. Nobody would design that on purpose, but a single {@code closed} flag
 * arrives at it by default.
 *
 * <p>ARCHIVED is not a status here. A closed campaign that is no longer interesting is
 * handled by {@code recordState} on the base class, the same as everywhere else.
 */
public enum FeedbackCampaignStatus {
    /** Being set up. Not visible to anybody who would submit to it. */
    DRAFT,

    /** Open for submissions. */
    OPEN,

    /** No more submissions accepted. Results not yet released to the subjects. */
    CLOSED,

    /** Results released to whoever the topic's visibility allows. */
    PUBLISHED,

    /** Called off. Any submissions already made stay, and are never released. */
    CANCELLED
}
