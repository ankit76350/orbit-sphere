package com.orbitastra.backend.dto.academics.attendance.response;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;

/**
 * One register in a list (attendance #7) — the header, and how many marks of each status it holds.
 */
public record AttendanceSessionRowResponse(

        String attendanceSessionDocsId,

        LocalDate attendanceDate,

        String classDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,

        AttendanceSessionType sessionType,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String periodCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String teacherName,

        AttendanceSessionStatus status,

        /** How many children have a mark in it. */
        long markedCount,

        /** Marks per status — every status listed, zeros included. */
        Map<String, Long> markCounts,

        Long version) {
}
