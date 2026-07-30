package com.orbitastra.backend.models.new_new.crm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionRecommendation;
import com.orbitastra.backend.models.new_new.crm.enums.AdmissionReviewStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "admission_reviews")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_application_round_reviewer_uniq",
                def = "{'schoolId': 1, 'admissionApplicationDocsId': 1, 'reviewRound': 1, 'reviewerDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_reviewer_status_due_idx",
                def = "{'schoolId': 1, 'reviewerDocsId': 1, 'status': 1, 'dueAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionReview extends SchoolBase {

    // Example: "67aa15d9dc3f7d0077777777"
    private String admissionApplicationDocsId;

    // Example: 1
    @Builder.Default
    private Integer reviewRound = 1;

    // Example: "67aa15d9dc3f7d0088888888"
    private String reviewerDocsId;

    // Example: "ADMISSION_OFFICER"
    private String reviewerRole;

    // Example: AdmissionReviewStatus.PENDING
    @Builder.Default
    private AdmissionReviewStatus status = AdmissionReviewStatus.PENDING;

    // Example: 2026-03-15T17:00:00Z
    private Instant dueAt;

    // Example: 2026-03-14T11:30:00Z
    private Instant completedAt;

    // Example: 86.50
    private BigDecimal score;

    // Example: AdmissionRecommendation.APPROVE
    private AdmissionRecommendation recommendation;

    // Example: "The applicant performed well in the interaction."
    private String notes;

    // Example: { "INTERVIEW": 42.50, "ENTRANCE_TEST": 44.00 }
    @Builder.Default
    private Map<String, BigDecimal> criterionScores = new HashMap<>();
}
