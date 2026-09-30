package com.orbitastra.backend.models.crm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One evaluation performed against an {@link AdmissionApplication}.
 *
 * <p>An application can have multiple reviews, multiple rounds, and multiple
 * reviewers. {@code admissionApplicationDocsId} references the application and
 * {@code reviewerDocsId} references the reviewing staff member. Reviews are
 * stored separately so the application remains small and the complete decision
 * history is retained.
 */
@Document(collection = "admission_reviews")
@CompoundIndexes({
                @CompoundIndex(name = "school_application_round_reviewer_uniq", def = "{'schoolId': 1, 'admissionApplicationDocsId': 1, 'reviewRound': 1, 'reviewerDocsId': 1}", unique = true),
                @CompoundIndex(name = "school_reviewer_status_due_idx", def = "{'schoolId': 1, 'reviewerDocsId': 1, 'status': 1, 'dueAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionReview extends SchoolBase {
        // This stores how the school evaluates an application.

        // Links to AdmissionApplication.id. Example: "67aa15d9dc3f7d0077777777"
        @NotBlank
        private String admissionApplicationDocsId;

        // Example: 1
        @NotNull
        @Builder.Default
        private Integer reviewRound = 1;

        // Links to the reviewing staff document. Example: "67aa15d9dc3f7d0088888888"
        @NotBlank
        private String reviewerDocsId;

        // Example: "ADMISSION_OFFICER"
        @NotBlank
        private String reviewerRole;


        // What each part of the assessment came to, as the school writes it.
        // Example: { "INTERVIEW": "42.50", "ENTRANCE_TEST": "B+", "READING": "Pass" }
        //
        // A STRING, NOT A NUMBER, from 2026-09-30. A criterion result is not always a figure: a
        // school grades an interview A/B/C, marks a reading check Pass or Fail, and writes "42/50"
        // for a paper. Holding these as BigDecimal refused every one of those, which is the same
        // mistake the score field avoided by having no upper bound — the scale is the school's.
        //
        // WHAT THAT COSTS: nothing checks the value any more. "abc" is storable where it used to
        // be 400 MALFORMED_REQUEST from the JSON reader, and no total can be computed across the
        // map without parsing it first. Neither was being done; if a report ever needs to add
        // these up, it is the thing that has to decide what a non-numeric criterion means.
        @Builder.Default
        private Map<String, String> criterionScores = new HashMap<>();

        // Example: 86.50
        private BigDecimal score;

        // Example: "The applicant performed well in the interaction."
        private String notes;

        // Example: AdmissionRecommendation.APPROVE
        private AdmissionRecommendation recommendation;

        // Example: AdmissionReviewStatus.PENDING
        @NotNull
        @Builder.Default
        private AdmissionReviewStatus status = AdmissionReviewStatus.PENDING;

        // Example: 2026-03-15T17:00:00Z
        private Instant dueAt;

        // Example: 2026-03-14T11:30:00Z
        private Instant completedAt;
}
