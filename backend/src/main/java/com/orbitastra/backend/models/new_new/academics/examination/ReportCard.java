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
 * Versioned, publishable snapshot of a student's results for one reporting
 * period. Published snapshots are not recalculated in place.
 */
@Document(collection = "report_cards")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_period_report_version_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'reportingPeriodName': 1, 'reportVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_report_status_published_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'publishedAt': -1}"),
        @CompoundIndex(
                name = "school_student_report_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'academicYear': -1, 'reportingPeriodName': 1, 'reportVersion': -1}")
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

    // School-defined term/reporting period name. Example: "Term 1"
    @NotBlank
    private String reportingPeriodName;

    // Starts at 1; corrections create a new version. Example: 1
    @NotNull
    @Builder.Default
    private Integer reportVersion = 1;

    // Exams included in the calculation.
    // Example: ["67aa15d9dc3f7d0022222222"]
    @Builder.Default
    private List<String> examDocsIds = new ArrayList<>();

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

    // Example: 94.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal attendancePercentage;

    // Example: "Excellent progress; continue regular revision."
    private String classTeacherRemark;

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
