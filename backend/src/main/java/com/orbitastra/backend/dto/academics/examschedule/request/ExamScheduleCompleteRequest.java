package com.orbitastra.backend.dto.academics.examschedule.request;

import jakarta.validation.constraints.NotNull;

/**
 * What examination #16 needs to close a paper's hall.
 *
 * @param version <b>the paper's version</b>, as #14 last gave it. A register saved since is
 *        refused, so nobody closes a hall they have not seen.
 */
public record ExamScheduleCompleteRequest(@NotNull Long version) {
}
