package com.orbitastra.backend.models.academics.enums;

/**
 * Where a student's mark is. Two states, and every mark on a paper moves between them together.
 *
 * <p>{@code DRAFT} while the teacher is entering and correcting it; {@code PUBLISHED} once the
 * paper's marks are final — what report cards are built from. A revaluation takes a paper's marks
 * back to {@code DRAFT}.
 */
public enum MarkEntryStatus {

    /** Being entered — can still be changed. */
    DRAFT,

    /** Final. No longer changed by entering marks; report cards read these. */
    PUBLISHED
}
