package com.orbitastra.backend.models.new_new.compliance.enums;

/**
 * How far one round of a requirement has got.
 *
 * <p>OVERDUE is a real state rather than something worked out from the date on every read. A
 * nightly job moves rows into it, so the list of things the school is late on is a plain
 * query and the day something became late is on the record.
 *
 * <p>REJECTED is kept apart from OVERDUE on purpose. Filed and refused is a different
 * problem from never filed: one needs correcting and refiling, the other needs somebody to
 * start.
 */
public enum SubmissionStatus {
    /** Due, nobody has started. */
    NOT_STARTED,

    /** Being prepared. */
    IN_PROGRESS,

    /** Sent to the authority and waiting on them. */
    SUBMITTED,

    /** Accepted. Finished. */
    ACCEPTED,

    /** Sent back by the authority. Needs correcting and sending again. */
    REJECTED,

    /** Past the due date and not yet submitted. */
    OVERDUE,

    /** No longer required, with a reason recorded. */
    WAIVED
}
