package com.orbitastra.backend.dto.academics.studentmark.request;

import jakarta.validation.constraints.NotNull;

/**
 * What examination #22 needs to publish a paper's marks.
 *
 * @param version <b>the paper's version</b>, as #18 last gave it. A paper saved since is refused,
 *        so nobody publishes marks they have not seen.
 */
public record StudentMarksPublishRequest(@NotNull Long version) {
}
