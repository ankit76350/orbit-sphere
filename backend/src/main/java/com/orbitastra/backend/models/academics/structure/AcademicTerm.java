package com.orbitastra.backend.models.new_new.academics.structure;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

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
 * One reporting period of an academic year, such as a term or semester.
 *
 * <p>This collection replaces the earlier free-text {@code reportingPeriodName}
 * used by Exam and ReportCard. A term is referenced by {@code termDocsId}, so a
 * renamed term does not orphan the exams and report cards that belong to it.
 *
 * <p>{@code weightPercent} is this term's contribution to the annual result.
 * Weighting therefore has exactly two levels: {@code Exam.weightPercent} inside
 * a term, and {@code AcademicTerm.weightPercent} inside the year. Both are
 * optional; when a school does not weight results, both stay null and totals are
 * aggregated from raw marks.
 *
 * <p>{@code resultsLocked} blocks result changes for this term only. The
 * year-wide {@code AcademicYear.resultsLocked} remains the stronger control and
 * overrides this field.
 *
 * <p>Date ordering, non-overlapping term ranges, sequence uniqueness, and the
 * rule that active term weights sum to 100 are service and request-DTO rules.
 */
@Document(collection = "academic_terms")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_term_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'termCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_term_sequence_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'sequence': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_term_active_dates_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'active': 1, 'startDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTerm extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable school-scoped key within the academic year. Example: "TERM_1"
    @NotBlank
    private String termCode;

    // Display name preserved by report-card snapshots. Example: "Term 1"
    @NotBlank
    private String name;

    // Ordering of terms inside the academic year. Example: 1
    @NotNull
    private Integer sequence;

    // Example: 2026-04-01
    @NotNull
    private LocalDate startDate;

    // Example: 2026-09-30
    @NotNull
    private LocalDate endDate;

    // This term's share of the annual result; null means no annual weighting.
    // Example: 20.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal weightPercent;

    // Blocks result changes for this term only. Example: false
    @NotNull
    @Builder.Default
    private Boolean resultsLocked = false;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
