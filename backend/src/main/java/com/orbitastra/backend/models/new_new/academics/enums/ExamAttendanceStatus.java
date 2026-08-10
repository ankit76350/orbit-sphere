package com.orbitastra.backend.models.new_new.academics.enums;

/**
 * What the invigilator observed for one student in the examination hall.
 *
 * <p>This records physical presence only. Late arrival and early departure are
 * derived from {@code ExamAttendance.reportedAt} and
 * {@code ExamAttendance.submittedAt} rather than from separate states.
 *
 * <p>Result-side states such as exemption or a withheld result belong to
 * {@code StudentMark.participationStatus}.
 */
public enum ExamAttendanceStatus {
    /** Student appeared and was issued an answer copy. */
    PRESENT,

    /** Student did not appear; no answer copy was issued. */
    ABSENT,

    /** Student appeared but was reported for unfair means; the copy was seized. */
    UNFAIR_MEANS
}
