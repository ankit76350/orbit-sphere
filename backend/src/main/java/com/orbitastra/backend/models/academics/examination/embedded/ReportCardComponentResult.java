package com.orbitastra.backend.models.academics.examination.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One component mark inside a {@link ReportCardSubjectResult} snapshot, such as
 * the theory or practical part of a subject.
 *
 * <p>This exists so a published report card can print the component breakdown —
 * "Theory 72/80, Practical 18/20" — without reading back StudentMark rows or the
 * ExamSchedule that defined the component.
 *
 * <p><b>One entry per paper, across every exam in the term.</b> A term card can combine a unit
 * test and the half-yearly, both with a Maths paper, so each entry names its exam and that
 * exam's weight. When marks are missing, {@code attendanceStatus} and {@code debarred} say why,
 * so the card can print "absent", "exempt", "withheld" or "debarred" rather than a blank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardComponentResult {

    // Links to Exam.id — which exam this paper belonged to. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String examDocsId;

    // Exam name snapshotted for printing. Example: "Half Yearly Examination, Term 1"
    @NotBlank
    private String examName;

    // The exam's share of the term result when the card was made; null when unweighted.
    // Example: 40.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal weightPercent;

    // Copied from ExamSchedule.componentName. Empty when the subject had one paper.
    // Example: "Theory"
    private String componentName;

    // Example: 80.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumMarks;

    // The effective mark — the evaluator's award plus any grace. Null when the
    // child did not sit the paper; attendanceStatus and debarred say why.
    // Example: 72.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal obtainedMarks;

    // Grace included in obtainedMarks, so the card can print "72 (+2 grace)".
    // Null when none. Example: 2.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal graceMarks;

    // The hall status at this paper: PRESENT, ABSENT, UNFAIR_MEANS or EXEMPT.
    // Null only when the child was debarred and never entered the hall.
    // Example: ExamAttendanceStatus.PRESENT
    private ExamAttendanceStatus attendanceStatus;

    // True when the child was debarred from this paper. Example: false
    @Builder.Default
    private Boolean debarred = false;
}
