package com.orbitastra.backend.dto.academics.examschedule.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;

/**
 * The filters examination #11 takes, all optional and all combined with AND. Bound from the query
 * string, so a date that is not YYYY-MM-DD or a status that does not exist is a 400.
 */
public record ExamDatesheetRequest(

        /** Only this class's papers. */
        String classDocsId,

        /** Only this section's papers, ignoring upper and lower case. */
        String sectionNo,

        /** Only the papers on this day. */
        LocalDate date,

        /** Only papers in this status. Absent shows every paper, cancelled ones included and marked. */
        ExamScheduleStatus status) {
}
