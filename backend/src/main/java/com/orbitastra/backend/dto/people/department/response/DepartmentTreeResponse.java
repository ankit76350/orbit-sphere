package com.orbitastra.backend.dto.people.department.response;

import java.util.List;

/**
 * The org chart, nested. What {@code ?tree=true} answers with.
 *
 * <p><b>A different shape from the flat page, and that is the point of the parameter.</b> The
 * caller chooses which it gets, so it always knows which it is reading.
 *
 * <p><b>Not paged, and asking for a page is refused rather than ignored.</b> A page boundary in a
 * tree cuts children off their parents — what comes back is not a partial tree but a broken one.
 *
 * <p><b>Built from one flat read.</b> A school has tens of departments; a query per level would be
 * a storm for a structure that fits in memory comfortably.
 */
public record DepartmentTreeResponse(
        List<DepartmentNodeResponse> roots,

        /** Every unit the filter matched, at any depth — not the number of roots. */
        int totalElements,

        /** How many of those were lifted to the top because their parent was filtered out. */
        int liftedToTop,

        /** The deepest chain in the answer. 1 for a flat school, 0 for none at all. */
        int depth) {
}
