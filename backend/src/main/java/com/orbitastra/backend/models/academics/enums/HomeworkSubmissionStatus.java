package com.orbitastra.backend.models.academics.enums;

/** Lifecycle of one student's homework attempt. */
public enum HomeworkSubmissionStatus {
    DRAFT,
    SUBMITTED,
    LATE,
    RETURNED,
    RESUBMISSION_REQUIRED,
    GRADED
}
