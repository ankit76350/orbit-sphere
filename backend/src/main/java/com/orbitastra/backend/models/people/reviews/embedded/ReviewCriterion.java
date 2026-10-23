package com.orbitastra.backend.models.people.reviews.embedded;

import com.orbitastra.backend.models.people.reviews.enums.ReviewerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Scoring criterion embedded in a ReviewCycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCriterion {

    // Stable key within the cycle. Example: "TEACHING_QUALITY"
    @NotBlank
    private String criterionCode;

    // Example: "Teaching Quality"
    @NotBlank
    private String label;

    // Reviewer group allowed to score this criterion.
    @NotNull
    private ReviewerType reviewerType;

    // Contribution to the final score. Example: 40
    @NotNull
    private Integer weightPercent;

    // Maximum raw score. Example: 100
    @NotNull
    private Integer maximumScore;
}
