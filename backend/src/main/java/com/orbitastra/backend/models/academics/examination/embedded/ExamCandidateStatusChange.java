package com.orbitastra.backend.models.academics.examination.embedded;

import java.time.Instant;

import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamDebarmentReason;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One time a candidate was debarred or let back in.
 *
 * <p>It has no collection of its own. It lives on the candidate, newest last, so the office can
 * see "debarred for fees on the 10th, let back in on the 12th after paying" without searching
 * anywhere else.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCandidateStatusChange {

    // The status the candidate moved to. Example: ExamCandidateStatus.DEBARRED
    @NotNull
    private ExamCandidateStatus status;

    // Why, when the candidate was debarred. Empty when they were let back in.
    // Example: ExamDebarmentReason.FEES_DUE
    private ExamDebarmentReason reason;

    // What the office wrote. Required with OTHER. Example: "Term 2 fee of Rs 12,000 unpaid."
    private String note;

    // When it happened. Example: 2026-11-10T04:30:00Z
    @NotNull
    private Instant changedAt;

    // Links to the Staff.id who made the change, when the caller was signed in as staff.
    // Example: "67aa15d9dc3f7d0044444444"
    private String changedByDocsId;
}
