package com.orbitastra.backend.models.new_new.inventory.enums;

/**
 * Who or what stock was given to.
 *
 * <p>Says which collection {@code StockIssue.issuedToDocsId} points at. One issue
 * register covers everybody, rather than a separate one per kind of recipient.
 *
 * <p>DEPARTMENT and HOSTEL_ROOM are places rather than people, and both are needed. Ten
 * bedsheets go to a room, not to a child, and a box of chalk goes to the science
 * department rather than to whoever happened to collect it.
 */
public enum IssuedToType {
    /** A member of staff, who is personally answerable for it. */
    STAFF,

    /** A student. */
    STUDENT,

    /** A department, with the head of it answerable. */
    DEPARTMENT,

    /** A hostel room, for linen and room fittings. */
    HOSTEL_ROOM,

    /** A class or section, such as a set of textbooks. */
    CLASS,

    /** The kitchen, for food taken to be cooked. */
    KITCHEN
}
