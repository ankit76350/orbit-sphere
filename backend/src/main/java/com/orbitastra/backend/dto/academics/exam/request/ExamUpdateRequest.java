package com.orbitastra.backend.dto.academics.exam.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #2 takes to fix an exam. Every field except {@code version} is optional, and
 * a field left out is left as it is.
 *
 * <p><b>The term can only change while the exam has no papers.</b> Its papers sit inside the
 * exam's dates, so moving the exam to another term would leave them in the wrong term.
 *
 * <p><b>The weight cannot be cleared here</b> — send a new value to change it. The grading scheme
 * <i>can</i> be cleared, by sending an empty string.
 */
public record ExamUpdateRequest(

        /** A new name, unique in the year ignoring case. Blank is refused, not treated as a clear. */
        @Size(max = 150) String name,

        /** Move the exam to another term of this year. Only while it has no papers. */
        @Size(max = 60) String termDocsId,

        /** A new first day. Sending one date alone keeps the other as it is. */
        LocalDate startDate,

        /** A new last day. */
        LocalDate endDate,

        /** A new share of the term result, above 0 and at most 100. */
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal weightPercent,

        /** A new default grading scheme. An empty string removes it. */
        @Size(max = 60) String gradingSchemeDocsId,

        /** The exam's version, as last read. A changed exam is refused. */
        @NotNull Long version) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return name == null && termDocsId == null && startDate == null && endDate == null
                && weightPercent == null && gradingSchemeDocsId == null;
    }
}
