package com.orbitastra.backend.dto.academics.exam.request;

import jakarta.validation.constraints.NotNull;

/**
 * What examination #3 needs to publish an exam's datesheet.
 *
 * @param version <b>the exam's version</b>, as last read. An exam changed since is refused, so
 *        nobody publishes a datesheet they have not seen.
 */
public record ExamPublishRequest(@NotNull Long version) {
}
