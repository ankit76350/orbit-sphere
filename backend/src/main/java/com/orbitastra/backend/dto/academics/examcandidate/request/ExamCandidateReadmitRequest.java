package com.orbitastra.backend.dto.academics.examcandidate.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #46 needs to let a debarred child back in.
 *
 * @param note    what the office writes — "Fee paid this morning". Optional.
 * @param version <b>the candidate's version</b>, as last read. A candidate changed since is
 *                refused.
 */
public record ExamCandidateReadmitRequest(
        @Size(max = 500) String note,
        @NotNull Long version) {
}
