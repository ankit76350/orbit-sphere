package com.orbitastra.backend.models.people.reviews;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.people.reviews.embedded.ReviewCriterion;
import com.orbitastra.backend.models.people.reviews.enums.ReviewCycleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * School-defined staff review cycle for one academic year.
 */
@Document(collection = "staff_review_cycles")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_review_year_cycle_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'cycleCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_review_status_dates_idx",
                def = "{'schoolId': 1, 'status': 1, 'startDate': 1, 'endDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCycle extends SchoolBase {

    // Stores AcademicYear.name, never its document id. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable code within the academic year. Example: "ANNUAL_REVIEW"
    @NotBlank
    private String cycleCode;

    // Example: "Annual Staff Review"
    @NotBlank
    private String name;

    // Example: 2027-01-01
    @NotNull
    private LocalDate startDate;

    // Example: 2027-03-15
    @NotNull
    private LocalDate endDate;

    // Example: ReviewCycleStatus.OPEN
    @NotNull
    @Builder.Default
    private ReviewCycleStatus status = ReviewCycleStatus.DRAFT;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean anonymousPeerFeedback = false;

    // Embedded scoring configuration.
    @Builder.Default
    private List<ReviewCriterion> criteria = new ArrayList<>();
}
