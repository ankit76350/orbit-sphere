package com.orbitastra.backend.dto.academics.reportcard.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ReportCardStatus;
import com.orbitastra.backend.models.academics.enums.ResultStatus;
import com.orbitastra.backend.models.academics.examination.embedded.ReportCardSubjectResult;

/**
 * What examination #29b answers: one child's report card for a term as the marks and the register
 * make it — nothing is saved. Its fields are the body #29 takes, so a screen can fill a form with
 * them, let the school edit, and send them back.
 *
 * @param warnings  why the numbers may not be final yet — a paper not completed, marks not
 *                  published, weights that do not add up. The draft is still made
 * @param weighted  true when the term's exams are weighted, so every subject is a weighted
 *                  percentage out of 100
 * @param existing  the child's latest card for the term, if any
 * @param saveWill  what #29 will do: CREATE (no card yet), REPLACE_DRAFT (a DRAFT or REVIEWED card
 *                  is rewritten), NEW_VERSION (after a REVOKED one) or REFUSE_PUBLISHED
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportCardDraftResponse(
        String termDocsId,
        String termName,
        String studentAcademicRecordDocsId,
        String studentDocsId,
        String fullName,
        String admissionNo,
        String classDocsId,
        String className,
        String sectionNo,
        String rollNo,
        List<String> examDocsIds,
        List<String> examNames,
        boolean weighted,
        String gradingSchemeDocsId,
        String gradingSchemeName,
        List<ReportCardSubjectResult> subjectResults,
        BigDecimal totalMaximumMarks,
        BigDecimal totalObtainedMarks,
        BigDecimal overallPercentage,
        String overallGradeCode,
        ResultStatus resultStatus,
        Integer attendanceWorkingDays,
        Integer attendancePresentDays,
        List<String> warnings,
        Existing existing,
        String saveWill) {

    /** The child's latest card for this term. */
    public record Existing(String reportCardDocsId, int reportVersion, ReportCardStatus status) {
    }
}
