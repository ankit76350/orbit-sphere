package com.orbitastra.backend.models.new_new.support.enums;

/**
 * Who gives the extra help.
 *
 * <p>Kept because the two behave differently. A member of staff is on a timetable and their
 * sessions can be checked against it; an outside specialist visits, invoices, and may stop
 * coming without telling anybody. A plan that depends on somebody the school does not employ is
 * a plan worth watching.
 */
public enum SupportProviderType {
    /** A member of the school's own staff. */
    SCHOOL_STAFF,

    /** A visiting specialist the school arranges: a therapist, a special educator. */
    EXTERNAL_SPECIALIST,

    /** Somebody the family arranges and the school only records. */
    FAMILY_ARRANGED
}
