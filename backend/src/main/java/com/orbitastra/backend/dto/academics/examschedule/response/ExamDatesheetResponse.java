package com.orbitastra.backend.dto.academics.examschedule.response;

import java.time.LocalDate;
import java.util.List;

/**
 * An exam's datesheet (examination #11): every paper that matches, by date, start time, class and
 * section, each with its class, subject, rooms and invigilators named.
 */
public record ExamDatesheetResponse(

        String examDocsId,

        String examName,

        LocalDate startDate,

        LocalDate endDate,

        /** How many papers match the filters. */
        int paperCount,

        List<ExamScheduleResponse> papers) {
}
