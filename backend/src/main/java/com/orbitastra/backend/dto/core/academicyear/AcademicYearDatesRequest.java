package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;

/**
 * Moves an academic year's boundaries. Endpoint #19.
 *
 * <p>Send either date or both. An omitted date is left alone — this is a PATCH, and the field
 * that is not mentioned is not the one being moved.
 *
 * <p><b>{@code name} is deliberately not on this record.</b> Not optional, not ignored — absent.
 * A year's name is the string every other collection stores to point at it, so renaming one
 * orphans all of them silently. A request that includes a name is rejected by the service rather
 * than quietly dropped, because a caller who sent it believed something would happen.
 *
 * <p>Shrinking is the dangerous direction. Extending a year usually harms nothing; pulling a
 * boundary inwards can leave a holiday, and eventually an attendance record or an invoice,
 * sitting outside the year that owns it.
 */
public record AcademicYearDatesRequest(

        /** Example: 2026-04-01 */
        LocalDate startDate,

        /** Example: 2027-03-31 */
        LocalDate endDate) {

    public boolean isEmpty() {
        return startDate == null && endDate == null;
    }
}
