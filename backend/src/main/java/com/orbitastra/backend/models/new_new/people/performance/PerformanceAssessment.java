package com.orbitastra.backend.models.new_new.people.performance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.performance.enums.PerformanceAssessmentStatus;
import com.orbitastra.backend.models.new_new.people.performance.enums.PerformanceReviewerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One reviewer's assessment of one Staff member in a PerformanceCycle.
 *
 * <p>
 * The keyed reviewer lookup hash prevents duplicate responses while allowing
 * the system to hide reviewer identity for configured anonymous feedback.
 * {@code reviewerDocsId} is omitted when identity must not be
 * retained in this document.
 */
@Document(collection = "staff_performance_assessments")
@CompoundIndexes({
                @CompoundIndex(name = "school_cycle_subject_reviewer_uniq", def = "{'schoolId': 1, 'performanceCycleDocsId': 1, 'subjectStaffDocsId': 1, 'reviewerType': 1, 'reviewerLookupHash': 1}", unique = true),
                @CompoundIndex(name = "school_cycle_subject_status_idx", def = "{'schoolId': 1, 'performanceCycleDocsId': 1, 'subjectStaffDocsId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAssessment extends SchoolBase {

        // Links to PerformanceCycle.id.
        @NotBlank
        private String performanceCycleDocsId;

        // Links to the Staff member being assessed.
        // Anita Sharma's Staff.id
        @NotBlank
        private String subjectStaffDocsId;

        // Example: PerformanceReviewerType.MANAGER
        // SELF → Anita reviews herself
        // MANAGER → Principal reviews Anita
        // PEER → Another teacher reviews Anita
        // STUDENT → Student reviews Anita
        // PARENT → Parent reviews Anita
        @NotNull
        private PerformanceReviewerType reviewerType;

        //! Optional Staff, Student, Guardian, or identity-account document id.
        private String reviewerDocsId;

        // Keyed HMAC used for response uniqueness and anonymity.
        @NotBlank
        private String reviewerLookupHash;

        // Example: PerformanceAssessmentStatus.SUBMITTED
        @NotNull
        @Builder.Default
        private PerformanceAssessmentStatus status = PerformanceAssessmentStatus.DRAFT;

        // Example: 2027-02-15T10:00:00Z
        // Stores when the reviewer submitted the assessment.
        private Instant submittedAt;

        // Example: {"TEACHING_QUALITY": 90.0, "COLLABORATION": 83.0}
        @Builder.Default
        private Map<String, BigDecimal> criterionScores = new HashMap<>();

        // Example: 86.50
        // Stores the final calculated score after applying criterion weights.
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal weightedScore;

        // Example: "Demonstrates strong classroom leadership."
        // Stores the reviewer’s written comments.
        private String narrative;
}
