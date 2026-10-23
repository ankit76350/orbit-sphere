package com.orbitastra.backend.dto.people.staff.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One person's whole employment history. Endpoint #19.
 *
 * <h2>An envelope, not a bare array</h2>
 *
 * <p>A top-level JSON array cannot grow a field without breaking every caller, and this one has
 * two worth having: whether the person is employed <b>right now</b>, and how many records there
 * are. Both are questions a caller would otherwise answer by scanning the list — and the first is
 * the question the list is usually opened to ask.
 *
 * <h2>Newest first</h2>
 *
 * <p>"What do they do now" is the common question and "what did they do in 2019" is the rare one,
 * so the answer to the first should not be at the bottom of the page. The current record is also
 * marked on each row, so a caller does not have to infer it from position.
 *
 * <h2>Not paged</h2>
 *
 * <p>Nobody has a hundred employment records. A cursor on a five-row list is machinery nobody
 * uses — the argument the term list eventually lost, and this one wins.
 *
 * <h2>An empty history is a real answer</h2>
 *
 * <p>Somebody entered and never employed has none, which is the state #1 leaves them in and #18b
 * can return them to. <b>It is not the same as an unknown person</b>, which is a 404 — and that
 * distinction is the whole reason this endpoint reads the staff record before the history.
 */
public record EmploymentHistoryResponse(

        String staffDocsId,

        /** Newest {@code effectiveFrom} first. Empty for somebody never employed, never null. */
        List<EmploymentResponse> records,

        int totalRecords,

        /**
         * Whether one of those records is current.
         *
         * <p>At most one can be — {@code school_staff_current_employment_uniq} is unique and
         * partial on {@code current: true} — so this is a boolean rather than a count.
         */
        boolean currentlyEmployed,

        /** Why the list is empty, when it is. Absent otherwise. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note) {
}
