package com.orbitastra.backend.models.new_new.identity.enums;

/**
 * What kind of person a login belongs to.
 *
 * <p>An account is always for a real person who already exists in the system. This
 * field says which collection {@code UserAccount.personDocsId} points at, so the
 * account can be joined back to the staff member, parent or student it belongs to.
 *
 * <p>There is no type for a shared office login. Every account belongs to one
 * named person, because otherwise "who approved this" has no answer.
 */
public enum PersonType {
    /** A member of staff. Points at Staff.id. */
    STAFF,

    /** A parent or another guardian. Points at Guardian.id. */
    GUARDIAN,

    /** A student logging in for themself. Points at Student.id. */
    STUDENT
}
