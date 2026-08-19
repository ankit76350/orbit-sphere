package com.orbitastra.backend.models.new_new.library.enums;

/**
 * How far a request for a book that is out has got.
 *
 * <p>READY_FOR_COLLECTION is the state with a clock on it. A copy held for somebody who
 * never comes is a copy nobody else can borrow, so a hold that is not collected expires
 * and the queue moves on.
 */
public enum ReservationStatus {
    /** In the queue. Every copy is out. */
    WAITING,

    /** A copy has come back and is being held. The hold expires if nobody comes. */
    READY_FOR_COLLECTION,

    /** Turned into a loan. */
    COLLECTED,

    /** Was held and nobody came for it in time. */
    EXPIRED,

    /** The borrower no longer wants it. */
    CANCELLED
}
