package com.orbitastra.backend.models.crm.enums;

/**
 * What a school does about an application. Endpoint #20.
 *
 * <p><b>This is the only enum in this package that is not stored on a document.</b> It is what the
 * caller asks for; what happens is recorded in {@code AdmissionApplicationStatus}. It lives here
 * rather than beside the request because it is module vocabulary, and somebody reading the other
 * six enums to learn what this module can do should find it with them.
 *
 * <p><b>It is not {@code AdmissionRecommendation}, which has the same first four values.</b> A
 * recommendation is what <i>one reviewer suggests</i> and it is stored on their review; a decision
 * is what <i>the school does</i>, and the school may decide something no reviewer recommended. The
 * two would have to be split the moment either grew a value the other did not want — which this
 * one already has.
 */
public enum AdmissionDecision {

    /** The school will offer a seat. Moves the application to {@code APPROVED}. */
    APPROVE,

    /** The school will not. Moves it to {@code REJECTED}, and <b>the note is required</b>. */
    REJECT,

    /**
     * Neither yet — held for a seat. Moves it to {@code WAITLISTED}.
     *
     * <p>A waitlisted application is not finished: it can still be approved when a place comes
     * free, which is the one move out of it that matters.
     */
    WAITLIST,

    /**
     * The school wants something more from the family. Moves it to
     * {@code ADDITIONAL_INFORMATION_REQUIRED}, and <b>the note is required</b> — asking for more
     * without saying what tells the family nothing.
     */
    REQUEST_MORE_INFORMATION,

    /**
     * What was asked for has arrived; carry on looking at it. Moves it back to
     * {@code UNDER_REVIEW}.
     *
     * <p><b>The fifth value, and the reason this is not {@code AdmissionRecommendation}.</b> The
     * status graph draws {@code ADDITIONAL_INFORMATION_REQUIRED → UNDER_REVIEW} and #26
     * deliberately does not make that move — assigning another reviewer is not what decides the
     * information turned up. Something had to own the edge, and no outcome describes it: resuming
     * a review is not a recommendation about a child.
     *
     * <p>It is the only value that is legal from exactly one status.
     */
    RESUME_REVIEW
}
