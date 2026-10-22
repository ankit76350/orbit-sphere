package com.orbitastra.backend.dto.academics.examcandidate.response;

import java.util.List;
import java.util.Map;

/**
 * What examination #44 answers: an exam's candidates in class, section and roll order, with the
 * totals. The totals are for the class and section asked for, before the status and reason
 * filters, so "4 debarred of 40" still reads right while only the debarred four are listed.
 */
public record ExamCandidateListResponse(

        String examDocsId,

        String examName,

        /** Every candidate in the class and section asked for — or the whole exam. */
        long total,

        long eligibleCount,

        long debarredCount,

        /** How many are debarred for each reason. Every reason is listed, zero included. */
        Map<String, Long> debarredByReason,

        /** How many rows are in {@code candidates}, after the status and reason filters. */
        int listedCount,

        List<ExamCandidateResponse> candidates) {
}
