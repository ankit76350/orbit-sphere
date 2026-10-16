package com.orbitastra.backend.dto.academics.exam.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.orbitastra.backend.models.academics.enums.ExamType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #1 needs to create an exam.
 *
 * <p><b>No {@code academicYear}</b> — it comes from the {@code {year}} in the path. <b>No
 * status</b> — a new exam always starts as {@code DRAFT}, and publishing is its own endpoint.
 */
public record ExamCreateRequest(

        /** "Half Yearly Examination, Term 1". Unique in the year, ignoring upper and lower case. */
        @NotBlank @Size(max = 150) String name,

        /** The term the exam belongs to. Must be an active term of this year. */
        @NotBlank @Size(max = 60) String termDocsId,

        /** UNIT_TEST, FORMATIVE, SUMMATIVE, MIDTERM, FINAL, PRACTICAL, PRE_BOARD or BOARD. */
        @NotNull ExamType examType,

        /** First day of the exam window. Must be inside the term. */
        @NotNull LocalDate startDate,

        /** Last day of the exam window. Must be inside the term, and not before startDate. */
        @NotNull LocalDate endDate,

        /**
         * This exam's share of the term result, above 0 and at most 100. Leave it out when the
         * school does not weight exams. Whether the term's weights add up to 100 is checked when
         * report cards are made, because exams are added one at a time.
         */
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal weightPercent,

        /** The grading scheme to use when a subject has none of its own. Optional. */
        @Size(max = 60) String gradingSchemeDocsId) {
}
