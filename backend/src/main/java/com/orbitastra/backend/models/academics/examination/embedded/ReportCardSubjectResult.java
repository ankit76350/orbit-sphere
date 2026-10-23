package com.orbitastra.backend.models.academics.examination.embedded;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.academics.enums.SubjectType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable subject-result snapshot embedded in one ReportCard version.
 *
 * <p>Every marks field is optional so a grade-only subject, such as a
 * co-scholastic activity assessed as A/B/C, can be recorded with
 * {@code gradeCode} alone.
 *
 * <p>{@code countsTowardTotal} controls aggregation and is deliberately separate
 * from {@code subjectType}, which is only for grouping on the printed card. A
 * school may assess an activity subject with marks and still exclude it from the
 * overall total, or include it; the flag records which choice was applied to this
 * snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardSubjectResult {

    // References SchoolClass.subjects[].subjectCode. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Preserved display name. Example: "Mathematics"
    @NotBlank
    private String subjectName;

    // Grouping on the printed card. Example: SubjectType.CORE
    private SubjectType subjectType;

    // Whether this subject was included in the ReportCard totals. Example: true
    @NotNull
    @Builder.Default
    private Boolean countsTowardTotal = true;

    // One entry per paper behind this subject, across every exam of the term —
    // a unit test and the half-yearly, theory and practical. Kept because real
    // cards print these columns, a failed practical must show, revaluation must
    // not change a printed card, and a missing mark says why here (absent,
    // exempt, debarred). Empty for a grade-only subject.
    @Builder.Default
    private List<ReportCardComponentResult> components = new ArrayList<>();

    // Example: 200.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumMarks;

    // Example: 172.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal obtainedMarks;

    // Example: 86.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentage;

    // Example: "A2"
    private String gradeCode;

    // GradingScheme.id that produced gradeCode — the subject's own scheme, else
    // the exam's. Example: "67aa15d9dc3f7d0077777777"
    private String gradingSchemeDocsId;

    // Outcome snapshotted so it is not re-derived from a grading scheme that may
    // since have been superseded. Null for subjects with no pass rule.
    // Example: true
    private Boolean passed;

    // Example: "Shows consistent progress in algebra."
    private String teacherRemark;
}
