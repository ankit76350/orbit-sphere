package com.orbitastra.backend.models.new_new.library.enums;

/**
 * How a loan is going, or how it ended.
 *
 * <p>OVERDUE is a state rather than something worked out from the date on every read.
 * A nightly job moves loans into it, which means the overdue list is a plain query and
 * the day a book became late is on the record instead of being recalculated differently
 * by every screen.
 *
 * <p>RETURNED_DAMAGED is kept apart from RETURNED because the book came back but the
 * school is still owed something for it. Treating them the same is how a damaged book
 * quietly becomes the library's problem instead of the borrower's.
 */
public enum BookLoanStatus {
    /** Out with the borrower, not yet late. */
    ON_LOAN,

    /** Out with the borrower and past the due date. */
    OVERDUE,

    /** Back on the shelf, nothing owed. */
    RETURNED,

    /** Back, but damaged, and something may be owed. */
    RETURNED_DAMAGED,

    /** The borrower has said it is lost, and is being charged for it. */
    LOST
}
