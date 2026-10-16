package com.orbitastra.backend.dto.academics.exam.response;

import java.util.Map;

/**
 * One exam in full (examination #7): the exam with its term and scheme named, and how far along
 * it is — its papers by status, and the marks entered against them by status.
 */
public record ExamDetailResponse(

        /** The exam itself, the same shape #1 and #6 return. */
        ExamResponse exam,

        /** Every paper of the exam, cancelled ones included. */
        long paperTotal,

        /** Papers per status — DRAFT, SCHEDULED, PUBLISHED, COMPLETED, CANCELLED. Every status is listed, 0 included. */
        Map<String, Long> papersByStatus,

        /** Every mark entered for the exam. */
        long markTotal,

        /** Marks per status — DRAFT, SUBMITTED, LOCKED, PUBLISHED. Every status is listed, 0 included. */
        Map<String, Long> marksByStatus) {
}
