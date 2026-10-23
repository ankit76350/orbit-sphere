package com.orbitastra.backend.dto.academics.reportcard.request;

import java.math.BigDecimal;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ResultStatus;
import com.orbitastra.backend.models.academics.enums.SubjectType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #29 needs to save one child's report card for a term — usually the draft #29b
 * worked out, edited where the school wanted. What is sent is what is stored; who the card belongs
 * to — the student record in the path, and its class, section and roll number, and the term's name —
 * is the server's.
 *
 * @param termDocsId            the term — a term of this year
 * @param examDocsIds           the exams the card was built from
 * @param gradingSchemeDocsId   the scheme behind the overall grade. Optional
 * @param subjectResults        one per subject, each with its papers as components
 * @param resultStatus          PASS, FAIL, WITHHELD or INCOMPLETE
 */
public record ReportCardSaveRequest(
        @NotBlank @Size(max = 60) String termDocsId,
        @Size(max = 50) List<@NotBlank @Size(max = 60) String> examDocsIds,
        @Size(max = 60) String gradingSchemeDocsId,
        @NotNull @Size(max = 60) List<@NotNull @Valid Subject> subjectResults,
        @Min(0) BigDecimal totalMaximumMarks,
        @Min(0) BigDecimal totalObtainedMarks,
        @Min(0) @Max(100) BigDecimal overallPercentage,
        @Size(max = 20) String overallGradeCode,
        @NotNull ResultStatus resultStatus,
        @Min(0) Integer attendanceWorkingDays,
        @Min(0) Integer attendancePresentDays,
        @Size(max = 1000) String classTeacherRemark,
        @Size(max = 1000) String principalRemark) {

    /**
     * One subject on the card. Marks are optional so a grade-only subject can carry a grade alone.
     *
     * @param countsTowardTotal whether the subject is in the card's total
     * @param passed            null for a subject with no pass rule
     */
    public record Subject(
            @NotBlank @Size(max = 40) String subjectCode,
            @NotBlank @Size(max = 120) String subjectName,
            SubjectType subjectType,
            @NotNull Boolean countsTowardTotal,
            @Size(max = 40) List<@NotNull @Valid Component> components,
            @Min(0) BigDecimal maximumMarks,
            @Min(0) BigDecimal obtainedMarks,
            @Min(0) @Max(100) BigDecimal percentage,
            @Size(max = 20) String gradeCode,
            @Size(max = 60) String gradingSchemeDocsId,
            Boolean passed,
            @Size(max = 500) String teacherRemark) {
    }

    /**
     * One paper inside a subject.
     *
     * @param obtainedMarks    the effective mark — null when the child did not sit
     * @param attendanceStatus the hall status at the paper; null only when debarred
     */
    public record Component(
            @NotBlank @Size(max = 60) String examDocsId,
            @NotBlank @Size(max = 200) String examName,
            @Min(0) @Max(100) BigDecimal weightPercent,
            @Size(max = 120) String componentName,
            @Min(0) BigDecimal maximumMarks,
            @Min(0) BigDecimal obtainedMarks,
            @Min(0) BigDecimal graceMarks,
            ExamAttendanceStatus attendanceStatus,
            Boolean debarred) {
    }
}
