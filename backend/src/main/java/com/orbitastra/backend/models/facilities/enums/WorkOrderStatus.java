package com.orbitastra.backend.models.facilities.enums;

/**
 * Where one maintenance job has got to.
 *
 * <p>AWAITING_PARTS is the state a simpler design leaves out, and its absence is why
 * maintenance queues stop being believed. A job waiting six weeks for a pump nobody can source
 * is not a job somebody forgot, and lumping it in with IN_PROGRESS means the overdue list is
 * full of things the school is not actually failing to do. Once everything looks overdue,
 * nothing does.
 *
 * <p>CLOSED_UNRESOLVED is the failure state, and it is the honest one. Sometimes the answer is
 * that the roof cannot be fixed this year, or the part is not made any more, and the school has
 * decided to live with it. Without a state for that, the job either sits open forever making
 * the list useless, or gets marked COMPLETED — which is a lie that will be read as a fact the
 * next time somebody asks whether that classroom is safe. It carries a reason for exactly that
 * reason.
 *
 * <p>This is the same rule as `NOT_COMPLETED` on a support session, `NOT_RETURNED` on a book
 * and `SHORT_CLOSED` on a purchase order: **a failure needs somewhere of its own to live, or it
 * hides inside a success.**
 */
public enum WorkOrderStatus {
    /** Somebody has reported it. Nobody is on it yet. */
    REPORTED,

    /** Given to a person or a contractor. */
    ASSIGNED,

    /** Work has started. */
    IN_PROGRESS,

    /** Stalled waiting for a part or a contractor, through nobody's fault here. */
    AWAITING_PARTS,

    /** Done, and what was done is recorded. */
    COMPLETED,

    /** The school has decided it will not be done, and why is recorded. */
    CLOSED_UNRESOLVED,

    /** Raised by mistake, or the problem went away. */
    CANCELLED
}
