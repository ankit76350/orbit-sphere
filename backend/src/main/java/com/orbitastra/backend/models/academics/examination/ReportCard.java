package com.orbitastra.backend.models.new_new.academics.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.academics.enums.ReportCardStatus;
import com.orbitastra.backend.models.new_new.academics.enums.ResultStatus;
import com.orbitastra.backend.models.new_new.academics.examination.embedded.ReportCardSubjectResult;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Versioned, publishable snapshot of a student's results for one AcademicTerm.
 * Published snapshots are not recalculated in place.
 *
 * <p>Because this document is a permanent historical record, the values needed to
 * reprint it are copied in rather than resolved at read time: the term name, the
 * class and section placement, the roll number, the grading scheme that produced
 * the grades, and the attendance day counts. A later section transfer, class
 * rename, or grading-scheme version must not change what an issued report card
 * says.
 *
 * <p>{@code classRank} is stored for the same reason. A rank recalculated on read
 * would drift as other students' marks change, so it is frozen at publication.
 */
@Document(collection = "report_cards")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_term_report_version_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'termDocsId': 1, 'reportVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_report_status_published_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'publishedAt': -1}"),
        @CompoundIndex(
                name = "school_student_report_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'academicYear': -1, 'termDocsId': 1, 'reportVersion': -1}"),
        @CompoundIndex(
                name = "school_term_class_section_report_idx",
                def = "{'schoolId': 1, 'termDocsId': 1, 'classDocsId': 1, 'sectionNo': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCard extends AcademicStudentSchoolBase {

    // Links to the student's StudentAcademicRecord.id for this year.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // Links to AcademicTerm.id. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String termDocsId;

    // Term name snapshotted so a renamed term does not change an issued card.
    // Example: "Term 1"
    @NotBlank
    private String termName;

    // Class placement snapshotted at publication. Links to SchoolClass.id.
    // Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String classDocsId;

    // Class name snapshotted for printing. Example: "Grade 7"
    @NotBlank
    private String className;

    // References SchoolClass.sections[].sectionNo at publication. Example: "A"
    private String sectionNo;

    // Roll number held at publication. Example: "23"
    private String rollNo;

    // Starts at 1; corrections create a new version. Example: 1
    @NotNull
    @Builder.Default
    private Integer reportVersion = 1;

    // Exams included in the calculation.
    // Example: ["67aa15d9dc3f7d0022222222"]
    @Builder.Default
    private List<String> examDocsIds = new ArrayList<>();

    // GradingScheme.id that produced the grades in this snapshot. Recorded
    // because schemes are versioned and may be superseded later.
    // Example: "67aa15d9dc3f7d0077777777"
    private String gradingSchemeDocsId;

    // Snapshot of calculated subject results.
    @Builder.Default
    private List<ReportCardSubjectResult> subjectResults = new ArrayList<>();

    // Example: 800.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalMaximumMarks;

    // Example: 688.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalObtainedMarks;

    // Example: 86.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal overallPercentage;

    // Example: "A2"
    private String overallGradeCode;

    // Example: ResultStatus.PASS
    @NotNull
    @Builder.Default
    private ResultStatus resultStatus = ResultStatus.INCOMPLETE;

    // Position within the ranked group; null when the school does not rank.
    // Example: 12
    private Integer classRank;

    // Size of the group the rank was calculated against, so "12 of 45" stays
    // reproducible. Example: 45
    private Integer rankedStudentCount;

    // Total school days in the reporting period. Example: 200
    private Integer attendanceWorkingDays;

    // Days the student was present. Example: 189
    private Integer attendancePresentDays;

    // Example: 94.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal attendancePercentage;

    // Example: "Excellent progress; continue regular revision."
    private String classTeacherRemark;

    // Example: "A consistent and well-rounded performance."
    private String principalRemark;

    // Example: ReportCardStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    // Example: 2026-09-01T05:30:00Z
    private Instant publishedAt;

    // Links to the publishing Staff.id.
    // Example: "67aa15d9dc3f7d0033333333"
    private String publishedByDocsId;

    // Optionally links to the generated PDF DocumentRecord.id.
    // Example: "67aa15d9dc3f7d0044444444"
    private String generatedDocumentDocsId;
}
