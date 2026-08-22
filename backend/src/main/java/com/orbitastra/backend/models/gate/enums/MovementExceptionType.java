package com.orbitastra.backend.models.gate.enums;

/**
 * What was wrong about a movement that was recorded anyway.
 *
 * <p>The log always accepts what happened. If a child walks out of the gate with
 * no pass, refusing to write the row would leave no record that they left at all,
 * and that is the one moment a record is most needed. So the movement is written
 * and marked instead, and the mark puts it on a list somebody looks at.
 *
 * <p>This is an enum rather than free text on purpose. Typed by hand, the same
 * problem would arrive as NO_OUT_PASS from one screen and no_out_pass from
 * another, and the exceptions list would quietly split in two.
 */
public enum MovementExceptionType {
    /** A student left during school hours with no approved out pass. */
    NO_OUT_PASS,

    /** The card worked but the person holding it was somebody else. */
    CARD_HOLDER_MISMATCH,

    /** Somebody left without ever being recorded as arriving that day. */
    NO_MATCHING_ENTRY,

    /** Somebody was recorded as arriving twice with no leaving in between. */
    DUPLICATE_ENTRY,

    /** The card used had run past its expiry date. */
    EXPIRED_CARD,

    /** The card used was reported lost or had been taken back. */
    INACTIVE_CARD,

    /** A visitor was let in at a gate that does not allow visitors. */
    VISITOR_AT_WRONG_GATE,

    /** A visitor who is blocked was let in anyway. */
    BLOCKED_VISITOR_ADMITTED,

    /** A child was released to somebody not authorised to collect them. */
    UNAUTHORIZED_COLLECTOR,

    /** A row added later to put an earlier wrong one right. */
    MANUAL_CORRECTION,

    /** Something the values above do not cover; remarks explain it. */
    OTHER
}
