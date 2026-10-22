package com.orbitastra.backend.dto.academics.examcandidate.response;

import java.util.List;

/**
 * What examination #43 answers: who was added, and who was already on the list.
 */
public record ExamCandidatesAddedResponse(

        String examDocsId,

        String examName,

        String classDocsId,

        String className,

        String sectionNo,

        /** How many children were added now. */
        int addedCount,

        /** How many were already candidates, and were left as they are. */
        int alreadyCandidateCount,

        List<ExamCandidateResponse> added,

        List<ExamCandidateResponse> alreadyCandidates,

        String nextStep) {
}
