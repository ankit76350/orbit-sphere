package com.orbitastra.backend.models.academics.enums;

/**
 * Why a child was stopped from sitting an exam.
 *
 * <p>A closed list so the office can count debarments by cause. {@code OTHER} needs a note.
 */
public enum ExamDebarmentReason {
    /** School fees not paid. */
    FEES_DUE,

    /** Class attendance below what the school requires to sit the exam. */
    ATTENDANCE_SHORTAGE,

    /** A disciplinary decision. */
    DISCIPLINARY,

    /** Forms or documents the school asked for were not handed in. */
    DOCUMENTS_PENDING,

    /** Anything else. The note says what. */
    OTHER
}
