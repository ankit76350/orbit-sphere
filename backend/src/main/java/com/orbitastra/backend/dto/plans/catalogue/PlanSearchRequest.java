package com.orbitastra.backend.dto.plans.catalogue;

import java.util.List;

import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * Everything a caller can ask of the plan catalogue. Endpoint #8.
 *
 * <p><b>Every field is optional.</b> A bare {@code GET /platform/plans} is the first page of the
 * whole catalogue.
 *
 * <p>They combine with AND: {@code ?status=ACTIVE&publiclyAvailable=true} is the plans a school
 * could actually pick today. Only {@code status} is OR within itself, because "show me the live
 * and the draft ones" is one question.
 *
 * <p><b>One row per plan version</b>, not per plan. {@code PREMIUM} v1 and v2 are two documents
 * with two prices, and a school is on exactly one of them, so a catalogue that collapsed them
 * would hide the thing somebody opened it to see.
 */
public record PlanSearchRequest(

        /** Repeat the parameter for several. Example: {@code ?status=ACTIVE&status=DRAFT} */
        List<PlanStatus> statuses,

        /**
         * Exact, case-insensitive, and normalized the same way a code is on the way in — so
         * {@code ?planCode=premium-plus} finds {@code PREMIUM_PLUS}. Example: "PREMIUM"
         *
         * <p>This is how you see every version of one plan.
         */
        String planCode,

        /**
         * Partial, case-insensitive. Example: "premium" finds "Premium" and "Premium Plus".
         *
         * <p>Partial rather than exact because an exact-name filter is unusable: nobody types a
         * plan's full display name to find it.
         */
        String name,

        /** Only listed plans, or only unlisted ones. Example: true */
        Boolean publiclyAvailable,

        /**
         * Partial, case-insensitive, across <b>the code or the name</b>. Example: "prem"
         *
         * <p>The one box on a screen. {@code planCode} and {@code name} are there for when a
         * caller knows which of the two they are looking at.
         */
        String search,

        /** Zero-based. Defaults to 0. */
        Integer page,

        /** Defaults to 20, capped at 100. */
        Integer size,

        /** {@code field,direction} — for example {@code name,asc}. */
        String sort) {
}
