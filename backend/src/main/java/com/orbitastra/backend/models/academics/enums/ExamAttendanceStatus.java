package com.orbitastra.backend.models.academics.enums;

/**
 * What the invigilator observed for one student in the examination hall.
 *
 * <p>This records physical presence only. Late arrival and early departure are
 * derived from {@code ExamPaperAttendance.reportedAt} and
 * {@code ExamPaperAttendance.submittedAt} rather than from separate states.
 *
 * <p><b>This is the only record of whether a child sat a paper.</b> A {@code StudentMark}
 * exists only for a child who was {@code PRESENT}; absent, unfair means and exempt are read
 * from here, and debarred from the candidate's status when the paper was held.
 */
public enum ExamAttendanceStatus {
    /** Student appeared and was issued an answer copy. */
    PRESENT,

    /** Student did not appear; no answer copy was issued. */
    ABSENT,

    /** Student appeared but was reported for unfair means; the copy was seized. The result is withheld. */
    UNFAIR_MEANS,

    /** Student does not sit this paper — an elective they do not take, or a medical exemption. No answer copy. */
    EXEMPT
}
