package com.orbitastra.backend.dto.academics.examcandidate.request;

import com.orbitastra.backend.models.academics.enums.ExamDebarmentReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #45 needs to stop a child from sitting an exam.
 *
 * @param reason  why — FEES_DUE, ATTENDANCE_SHORTAGE, DISCIPLINARY, DOCUMENTS_PENDING or OTHER.
 * @param note    what the office writes. Optional, except with OTHER, where it says what the
 *                reason is.
 * @param version <b>the candidate's version</b>, as last read. A candidate changed since is
 *                refused, so nobody debars a child they have not seen.
 */
public record ExamCandidateDebarRequest(
        @NotNull ExamDebarmentReason reason,
        @Size(max = 500) String note,
        @NotNull Long version) {
}
