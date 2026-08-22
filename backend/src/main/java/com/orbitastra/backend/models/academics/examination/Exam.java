package com.orbitastra.backend.models.new_new.academics.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.academics.enums.ExamStatus;
import com.orbitastra.backend.models.new_new.academics.enums.ExamType;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One examination event, such as a unit test, midterm, or final examination.
 *
 * <p>An exam belongs to exactly one {@code AcademicTerm} through
 * {@code termDocsId}. {@code weightPercent} is this exam's contribution to that
 * term's result, so weighting needs only two levels: exam inside term, and term
 * inside year.
 */
@Document(collection = "exams")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_exam_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'examCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_exam_status_start_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'startDate': 1}"),
        @CompoundIndex(
                name = "school_term_exam_type_idx",
                def = "{'schoolId': 1, 'termDocsId': 1, 'examType': 1, 'startDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Exam extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable key within the academic year. Example: "MIDTERM_2026"
    @NotBlank
    private String examCode;

    // Example: "Midterm Examination 2026"
    @NotBlank
    private String name;

    // Links to AcademicTerm.id, the reporting period this exam belongs to.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String termDocsId;

    // Example: ExamType.MIDTERM
    @NotNull
    private ExamType examType;

    // This exam's share of the term result; null means no weighting inside the
    // term and raw marks are aggregated. Example: 30.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal weightPercent;

    // Example: 2026-08-10
    @NotNull
    private LocalDate startDate;

    // Example: 2026-08-20
    @NotNull
    private LocalDate endDate;

    // Optionally links to the default GradingScheme.id.
    // Example: "67aa15d9dc3f7d0011111111"
    private String gradingSchemeDocsId;

    // Example: ExamStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    // Example: 2026-07-20T08:30:00Z
    private Instant publishedAt;

    // Links to the publishing Staff.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String publishedByDocsId;
}
