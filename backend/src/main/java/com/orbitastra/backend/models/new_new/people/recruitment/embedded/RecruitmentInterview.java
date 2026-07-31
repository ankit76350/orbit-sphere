package com.orbitastra.backend.models.new_new.people.recruitment.embedded;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.people.recruitment.enums.InterviewRecommendation;
import com.orbitastra.backend.models.new_new.people.recruitment.enums.InterviewStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Interview event embedded in a RecruitmentApplication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInterview {

    // Unique key within the application. Example: "TECHNICAL_ROUND_1"
    @NotBlank
    private String interviewKey;

    // Example: 2026-08-15T10:30:00Z
    @NotNull
    private Instant scheduledAt;

    // Links to panel members' Staff.id values.
    @Builder.Default
    private List<String> panelistDocsIds = new ArrayList<>();

    // Example: InterviewStatus.COMPLETED
    @NotNull
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    // Example: 84.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal score;

    // Example: InterviewRecommendation.SELECT
    private InterviewRecommendation recommendation;

    // Example: "Strong subject knowledge and communication."
    private String notes;

    // Links to the completed scorecard document.
    private String scorecardDocumentDocsId;
}
