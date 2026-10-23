package com.orbitastra.backend.dto.academics.exam.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;

/**
 * What examination #26 answers: where every paper of an exam is with its marks — the exam office's
 * "who hasn't entered marks". One row per paper, by date, time, class and section.
 */
public record ExamMarkEntryResponse(

        String examDocsId,

        String examName,

        /** Every paper of the exam, cancelled ones included. */
        int paperCount,

        /** How many papers are still holding something up. */
        int pendingCount,

        /** Papers per stage, every stage listed. */
        Map<String, Integer> byStage,

        /** How many rows are listed — fewer than paperCount with ?pending=true. */
        int listedCount,

        List<Row> papers) {

    /**
     * One paper.
     *
     * @param sat        children marked PRESENT in its hall — the ones who take a mark
     * @param entered    of those, how many have a mark
     * @param notEntered of those, how many have none yet
     * @param draft      marks still DRAFT
     * @param published  marks PUBLISHED
     * @param stage      NOT_SAT_YET, HALL_NOT_CLOSED, MARKS_MISSING, READY_TO_PUBLISH, PUBLISHED,
     *                   NOTHING_TO_MARK or CANCELLED
     * @param pending    true for HALL_NOT_CLOSED, MARKS_MISSING and READY_TO_PUBLISH
     */
    public record Row(String examScheduleDocsId, String classDocsId,
            @JsonInclude(JsonInclude.Include.NON_NULL) String className, String sectionNo,
            String subjectCode, @JsonInclude(JsonInclude.Include.NON_NULL) String subjectName,
            @JsonInclude(JsonInclude.Include.NON_NULL) String componentName, LocalDate examDate,
            LocalTime startTime, ExamScheduleStatus status, int sat, int entered, int notEntered,
            int draft, int published, String stage, boolean pending) {
    }
}
