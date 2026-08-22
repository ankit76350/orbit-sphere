package com.orbitastra.backend.models.new_new.hostel.enums;

/**
 * Where one child was at one roll call.
 *
 * <p>UNACCOUNTED is the whole point of taking a roll call. Every other value is somebody
 * knowing where the child is. UNACCOUNTED is nobody knowing, and it is the state that
 * has to reach a warden and then a parent within minutes rather than sitting in a list.
 *
 * <p>ON_APPROVED_LEAVE and IN_CLINIC are kept apart from ABSENT for the same reason: a
 * child who is somewhere the school put them is not missing, and lumping the three
 * together is how a genuinely missing child gets lost in a column of absences.
 */
public enum RollCallStatus {
    /** Answered, and is in the building. */
    PRESENT,

    /** Away on leave the warden approved. */
    ON_APPROVED_LEAVE,

    /** In the school clinic or a hospital. */
    IN_CLINIC,

    /** Known to be away for a reason that is not leave, such as a match or a trip. */
    EXCUSED,

    /** Nobody knows where this child is. Act now. */
    UNACCOUNTED
}
