package com.orbitastra.backend.models.undone.exams.enums;


/**
 * Status of a student's examination entry for a subject.
 */
public enum ExamMarkStatus {

    /** Student appeared for the exam and marks were awarded. */
    PRESENT,

    /** Student did not appear for the exam. */
    ABSENT,

    /** Student was officially exempted from the exam. */
    EXEMPTED,

    /** Student's result was cancelled due to malpractice. */
    MALPRACTICE,

    /** Marks have not yet been entered or evaluated. */
    PENDING
}