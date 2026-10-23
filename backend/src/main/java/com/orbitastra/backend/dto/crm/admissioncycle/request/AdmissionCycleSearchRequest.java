package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.time.Instant;

import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

/**
 * What #5 may be asked for. Every field is optional; sending none returns the school's cycles,
 * newest year first.
 *
 * <p><b>Bound from the query string</b>, not a body — this is a GET.
 *
 * <p><b>There is no {@code schoolId} here, and there must never be one.</b> The school comes from
 * the caller's cookie. A search that took it from the request would let one school read another's
 * admissions by typing an id.
 */
public record AdmissionCycleSearchRequest(

        /**
         * One year's rounds. Example: "2026-2027"
         *
         * <p><b>Absent returns every year, and that is the normal case here</b> — unlike classes
         * or terms, where a year is always in the path. A school runs next year's admissions
         * during this one, so "show me both" is what the screen usually wants.
         */
        String academicYear,

        /**
         * One status. Example: OPEN
         *
         * <p>DRAFT finds the rounds nobody has opened yet; OPEN finds the ones taking applications
         * now. Absent returns every status, cancelled ones included.
         */
        AdmissionCycleStatus status,

        /**
         * Part of a cycle's name, matched anywhere in it and ignoring case. Example: "main"
         *
         * <p>A cycle has no code, so the name is all there is to search. What the caller types is
         * quoted before it becomes a regex, so "Main (2026)" looks for those characters instead of
         * being read as a group.
         */
        String search,

        /**
         * Which rounds were taking applications on this day. Example: "2026-07-15T00:00:00Z"
         *
         * <p>Matches a cycle whose {@code applicationOpenAt} is on or before this and whose
         * {@code applicationCloseAt} is on or after it.
         *
         * <p><b>A cycle missing either date does not match.</b> One with no close date is not open
         * forever — it is a cycle whose calendar was never filled in, and counting it would be
         * answering a question the data cannot answer.
         *
         * <p>This reads the stored dates. It does <b>not</b> mean applications would be accepted:
         * that is the cycle's status, and nothing enforces these dates yet.
         */
        Instant openOn,

        Integer page,
        Integer size,
        String sort) {
}
