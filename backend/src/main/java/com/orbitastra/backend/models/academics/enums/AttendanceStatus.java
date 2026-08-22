package com.orbitastra.backend.models.new_new.academics.enums;

/**
 * Recorded attendance state of one student.
 *
 * <p>Absence is split into authorised and unauthorised because that is the one
 * distinction every attendance report needs and it cannot be recovered from a
 * free-text reason afterwards. Detail beyond these four states goes in
 * {@code StudentAttendanceRecord.reason}, for example {@code "MEDICAL"},
 * {@code "BEREAVEMENT"}, or {@code "FAMILY_FUNCTION"}.
 *
 * <pre>
 * attendance %  = PRESENT (+ LATE, if school policy counts it) / marked sessions
 * </pre>
 */
public enum AttendanceStatus {

    /** Student attended the session. */
    PRESENT,

    /** Student attended but arrived after the register opened. */
    LATE,

    /** Absent with the school's approval, such as medical or approved leave. */
    AUTHORISED_ABSENCE,

    /** Absent without approval, including unexplained absence. */
    UNAUTHORISED_ABSENCE
}
