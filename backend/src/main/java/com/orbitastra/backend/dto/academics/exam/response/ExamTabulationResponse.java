package com.orbitastra.backend.dto.academics.exam.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.academics.enums.ResultStatus;

/**
 * What examination #27 answers: one exam's tabulation sheet for one section — a row per child, a
 * column per paper, then the child's total, percentage, grade and result. No rank.
 */
public record ExamTabulationResponse(

        String examDocsId,

        String examName,

        String classDocsId,

        String className,

        String sectionNo,

        /** The scheme the overall grade comes from — the exam's. Absent when the exam names none. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String gradingSchemeDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String gradingSchemeName,

        /** The section's papers, in datesheet order — the sheet's columns. */
        List<Column> columns,

        /** How many columns are final — their marks published. */
        int finalColumns,

        /** Children per result, every result listed. */
        Map<String, Integer> byResult,

        /** One row per child, in roll order. */
        List<Row> students) {

    /**
     * One paper.
     *
     * @param graded true for a grade-only paper — its cells carry a grade, not marks, and it is
     *               left out of the total
     * @param isFinal true once its marks are published
     */
    public record Column(String examScheduleDocsId, String subjectCode,
            @JsonInclude(JsonInclude.Include.NON_NULL) String subjectName,
            @JsonInclude(JsonInclude.Include.NON_NULL) String componentName, LocalDate examDate,
            BigDecimal maximumMarks, @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal passingMarks,
            ExamScheduleStatus status, boolean graded, boolean isFinal) {
    }

    /**
     * One child: a cell per column, then the totals.
     *
     * @param total        effective marks added up over the papers that count
     * @param maximumTotal the maximum of those papers
     * @param percentage   total over maximumTotal, out of 100. Absent when nothing counts
     * @param grade        the percentage resolved on the exam's scheme. Absent when there is no
     *                     scheme, or the scheme has no band for it
     * @param result       PASS, FAIL, WITHHELD or INCOMPLETE
     */
    public record Row(String studentDocsId, @JsonInclude(JsonInclude.Include.NON_NULL) String fullName,
            @JsonInclude(JsonInclude.Include.NON_NULL) String admissionNo,
            @JsonInclude(JsonInclude.Include.NON_NULL) String rollNo, ExamCandidateStatus candidateStatus,
            List<Cell> cells, BigDecimal total, BigDecimal maximumTotal,
            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal percentage,
            @JsonInclude(JsonInclude.Include.NON_NULL) String grade, ResultStatus result) {
    }

    /**
     * One child on one paper, in the columns' order.
     *
     * @param code   null when there is a mark; otherwise why there is none — AB absent, EX exempt,
     *               WH withheld (unfair means), DB debarred, NE sat but not entered, "—" not held yet
     * @param passed whether the effective marks reach the paper's passing marks. Absent when the
     *               paper has no passing marks, or there is no mark
     */
    public record Cell(String examScheduleDocsId, @JsonInclude(JsonInclude.Include.NON_NULL) String code,
            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal effectiveMarks,
            @JsonInclude(JsonInclude.Include.NON_NULL) String gradeCode,
            @JsonInclude(JsonInclude.Include.NON_NULL) MarkEntryStatus markStatus,
            @JsonInclude(JsonInclude.Include.NON_NULL) Boolean passed) {
    }
}
