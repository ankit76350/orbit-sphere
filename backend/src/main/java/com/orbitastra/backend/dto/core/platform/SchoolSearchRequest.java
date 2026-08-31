package com.orbitastra.backend.dto.core.platform;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.core.enums.SchoolStatus;

/**
 * Everything a caller can ask of the platform school list. Endpoint G1.
 *
 * <p><b>Every field is optional.</b> A bare {@code GET /platform/schools} is the first page of
 * every school, newest first, which is what an operator opening the console wants.
 *
 * <p>They combine with AND: {@code ?status=ACTIVE&countryCode=IN&search=st} is active Indian
 * schools whose name or subdomain contains "st". Only {@code status} is OR within itself —
 * {@code ?status=ACTIVE&status=TRIAL} means either, because "show me the live ones" is one
 * question and not two.
 *
 * <p>The raw request as it arrives, not a validated one. {@code page}, {@code size} and
 * {@code sort} are checked and turned into a {@code Pageable} in the service; a record cannot
 * refuse its own arguments with a sensible error code, and quietly clamping them in a compact
 * constructor would mean {@code size=5000} silently returning 100 rows as though that was what
 * was asked.
 */
public record SchoolSearchRequest(

        /** Repeat the parameter for several. Example: {@code ?status=ACTIVE&status=TRIAL} */
        List<SchoolStatus> statuses,

        /**
         * Partial, case-insensitive match on <b>school name or subdomain</b>. Example: "orbit"
         *
         * <p>Only those two. Searching the address or the account holder as well sounds
         * generous, but then a search for "pune" returns every school in the city and the box
         * stops being useful for finding one school.
         */
        String search,

        /** Exact, case-insensitive. Example: "IN" */
        String countryCode,

        /** Exact, case-insensitive. Example: "Pune" */
        String city,

        /** Schools created at or after this instant. Example: 2026-04-01T00:00:00Z */
        Instant createdFrom,

        /** Schools created at or before this instant. Example: 2026-08-31T23:59:59Z */
        Instant createdTo,

        /** Zero-based. Defaults to 0. */
        Integer page,

        /** Defaults to 20, capped at 100. */
        Integer size,

        /** {@code field,direction} — for example {@code name,asc}. Defaults to newest first. */
        String sort) {
}
