package com.orbitastra.backend.dto.academics.examschedule.response;

import java.util.List;

/**
 * What examination #8 answers: the papers it added, one per section.
 */
public record ExamPapersAddedResponse(

        String examDocsId,

        String examName,

        /** How many papers were added — one per section sent. */
        int paperCount,

        List<ExamScheduleResponse> papers,

        String nextStep) {
}
