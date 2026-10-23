package com.orbitastra.backend.dto.academics.studentmark.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;

/**
 * What examination #18 answers: one paper's mark sheet — every child with a hall entry, in roll
 * order. A child marked PRESENT carries their mark, or none yet; anyone else carries only their
 * hall status, because only a child who sat gets a mark.
 */
public record ExamMarkSheetResponse(

        String examScheduleDocsId,

        String examDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String examName,

        String classDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,

        String subjectCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String componentName,

        LocalDate examDate,

        BigDecimal maximumMarks,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal passingMarks,

        /** The paper's status. Marks are entered once it is COMPLETED. */
        ExamScheduleStatus status,

        /** The scheme this paper's grades come from. Absent when neither the subject nor the exam names one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Scheme gradingScheme,

        Counts counts,

        /** Every child with a hall entry, in roll order, then by name. */
        List<Row> students,

        /** The paper's version, for the next mark save (#19). */
        Long version) {

    /**
     * The grading scheme that applies — the class subject's, else the exam's.
     *
     * @param from SUBJECT or EXAM — where it was found
     */
    public record Scheme(String gradingSchemeDocsId, String name, GradingScaleType scaleType,
            List<String> gradeCodes, String from) {
    }

    /**
     * The sheet's numbers.
     *
     * @param listed       every row
     * @param present      rows whose hall status is PRESENT — the ones that take a mark
     * @param entered      present rows that have a mark
     * @param notEntered   present rows with no mark yet
     * @param byHallStatus rows per hall status, every status listed
     * @param byMarkStatus marks per mark status, every status listed
     */
    public record Counts(int listed, int present, int entered, int notEntered,
            Map<String, Integer> byHallStatus, Map<String, Integer> byMarkStatus) {
    }

    /**
     * One child on the sheet.
     *
     * @param canBeMarked true only for a child whose hall status is PRESENT
     * @param mark        their mark, or null when none is entered
     */
    public record Row(String studentDocsId, String fullName, String admissionNo, String rollNo,
            ExamAttendanceStatus attendanceStatus, boolean canBeMarked, Mark mark) {
    }

    /**
     * A stored mark. {@code effectiveMarks} is obtained plus grace, worked out here — the award
     * itself is never changed by grace.
     */
    public record Mark(String studentMarkDocsId, BigDecimal obtainedMarks, BigDecimal graceMarks,
            BigDecimal effectiveMarks, String gradeCode, MarkEntryStatus status, String remarks,
            Instant enteredAt, String enteredByDocsId, Long version) {
    }
}
