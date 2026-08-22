package com.orbitastra.backend.models.new_new.transport.enums;

/**
 * What happened to one student on one trip.
 *
 * <p>A row starts as EXPECTED when the trip list is made, and is meant to end as
 * COMPLETED. Anything else is worth somebody looking at.
 *
 * <p>MISSED and NOT_TRAVELLING are deliberately different. NOT_TRAVELLING is the
 * family telling the school in advance. MISSED is nobody knowing where the child
 * is, and that is the one that has to reach a parent quickly.
 */
public enum BoardingStatus {
    /** On the list for this trip, nothing recorded yet. */
    EXPECTED,

    /** Got on the bus. */
    BOARDED,

    /** Got on and got off again. The normal ending. */
    COMPLETED,

    /** Did not get on, and nobody said they would not. */
    MISSED,

    /** The family said in advance the child would not travel. */
    NOT_TRAVELLING
}
