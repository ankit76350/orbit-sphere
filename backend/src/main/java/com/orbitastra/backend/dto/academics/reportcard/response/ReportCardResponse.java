package com.orbitastra.backend.dto.academics.reportcard.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ReportCardStatus;
import com.orbitastra.backend.models.academics.enums.ResultStatus;
import com.orbitastra.backend.models.academics.examination.ReportCard;
import com.orbitastra.backend.models.academics.examination.embedded.ReportCardSubjectResult;
import com.orbitastra.backend.models.student.Student;

/**
 * One saved report card in full — what gets printed — with the child's name beside it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportCardResponse(
        String reportCardDocsId,
        String studentDocsId,
        String fullName,
        String admissionNo,
        String studentAcademicRecordDocsId,
        String termDocsId,
        String termName,
        String classDocsId,
        String className,
        String sectionNo,
        String rollNo,
        Integer reportVersion,
        ReportCardStatus status,
        List<String> examDocsIds,
        String gradingSchemeDocsId,
        List<ReportCardSubjectResult> subjectResults,
        BigDecimal totalMaximumMarks,
        BigDecimal totalObtainedMarks,
        BigDecimal overallPercentage,
        String overallGradeCode,
        ResultStatus resultStatus,
        Integer classRank,
        Integer rankedStudentCount,
        Integer attendanceWorkingDays,
        Integer attendancePresentDays,
        String classTeacherRemark,
        String principalRemark,
        Instant reviewedAt,
        Instant publishedAt,
        String revocationReason,
        /** For the next write to this card. */
        Long version,
        String nextStep) {

    /** Builds the answer from the saved card and the child (either may lack a name). */
    public static ReportCardResponse of(ReportCard card, Student child, String nextStep) {
        return new ReportCardResponse(card.getId(), card.getStudentDocsId(),
                child == null ? null : child.getFullName(), child == null ? null : child.getAdmissionNo(),
                card.getStudentAcademicRecordDocsId(), card.getTermDocsId(), card.getTermName(),
                card.getClassDocsId(), card.getClassName(), card.getSectionNo(), card.getRollNo(),
                card.getReportVersion(), card.getStatus(), card.getExamDocsIds(), card.getGradingSchemeDocsId(),
                card.getSubjectResults(), card.getTotalMaximumMarks(), card.getTotalObtainedMarks(),
                card.getOverallPercentage(), card.getOverallGradeCode(), card.getResultStatus(),
                card.getClassRank(), card.getRankedStudentCount(), card.getAttendanceWorkingDays(),
                card.getAttendancePresentDays(), card.getClassTeacherRemark(), card.getPrincipalRemark(),
                card.getReviewedAt(), card.getPublishedAt(), card.getRevocationReason(), card.getVersion(),
                nextStep);
    }
}
