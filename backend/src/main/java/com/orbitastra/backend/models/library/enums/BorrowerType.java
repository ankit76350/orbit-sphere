package com.orbitastra.backend.models.library.enums;

/**
 * Who is borrowing.
 *
 * <p>Says which collection {@code borrowerDocsId} points at, so one issue register
 * covers the whole school rather than needing a separate one for staff.
 *
 * <p>It also picks the LibraryPolicy: a teacher normally keeps a book longer and may
 * hold more at once than a Class III child.
 */
public enum BorrowerType {
    /** A student. Points at Student.id. */
    STUDENT,

    /** A member of staff. Points at Staff.id. */
    STAFF
}
