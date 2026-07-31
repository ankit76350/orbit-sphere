package com.orbitastra.backend.models.new_new.people.performance.embedded;

import com.orbitastra.backend.models.new_new.people.performance.enums.PerformanceRespondentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Scoring criterion embedded in a PerformanceCycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceCriterion {

    // Stable key within the cycle. Example: "TEACHING_QUALITY"
    @NotBlank
    private String criterionCode;

    // Example: "Teaching Quality"
    @NotBlank
    private String label;

    // Respondent group allowed to score this criterion.
    @NotNull
    private PerformanceRespondentType respondentType;

    // Contribution to the final score. Example: 40
    @NotNull
    private Integer weightPercent;

    // Maximum raw score. Example: 100
    @NotNull
    private Integer maximumScore;
}
