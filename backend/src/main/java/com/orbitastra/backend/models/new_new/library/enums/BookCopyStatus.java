package com.orbitastra.backend.models.new_new.library.enums;

/**
 * Whether one physical copy can be lent right now.
 *
 * <p>Only AVAILABLE may be issued. The point of keeping this on the copy rather than
 * working it out from the loans is the shelf: a librarian looking for a book needs to
 * know in one read whether it should be there, without searching the loan register
 * for every copy.
 */
public enum BookCopyStatus {
    /** On the shelf and free to lend. */
    AVAILABLE,

    /** Somebody has it. */
    ON_LOAN,

    /** Held back for whoever is next in the queue. */
    RESERVED,

    /** Being mended or rebound. */
    IN_REPAIR,

    /** Nobody knows where it is. */
    LOST,

    /** Taken off the register for good, whether worn out or given away. */
    WITHDRAWN
}
