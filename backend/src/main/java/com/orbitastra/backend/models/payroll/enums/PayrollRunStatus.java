package com.orbitastra.backend.models.payroll.enums;

/**
 * How far one month's payroll has got.
 *
 * <p>COMPUTED and APPROVED are deliberately separate. Computing works out what everybody
 * should be paid and is safe to do as often as you like; approving is somebody agreeing to
 * it, and after that the figures must not move. Merging the two would mean every
 * recalculation silently re-approved itself.
 *
 * <p>Nothing may be paid before APPROVED, and once PAID the run is closed for good. A
 * mistake found after payment is corrected in the next month's run rather than by
 * reopening a month somebody has already been paid for.
 */
public enum PayrollRunStatus {
    /** The month exists and nothing has been worked out yet. */
    DRAFT,

    /** Everybody's pay has been worked out. Safe to run again. */
    COMPUTED,

    /** Sent for approval and waiting. */
    PENDING_APPROVAL,

    /** Agreed. The figures are now fixed and payment may go out. */
    APPROVED,

    /** The money has been paid. Closed for good. */
    PAID,

    /** Abandoned before anybody was paid, with a reason recorded. */
    CANCELLED
}
