package com.orbitastra.backend.models.undone.compliance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
import com.orbitastra.backend.models.undone.compliance.enums.HpcLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NEP 2020 Holistic Progress Card — a 360° developmental report across learning
 * domains with self/peer/parent feedback, distinct from the marks-based
 * {@code academics.AcademicResult}.
 */
@Document(collection = "holistic_progress_cards")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HolisticProgressCard extends BaseDocument {

    @Indexed
    private String studentId;

    private String grade;

    private String term;

    // Per-domain attainment (physical, socio-emotional, cognitive, language, aesthetic).
    @Builder.Default
    private List<DomainAssessment> domains = new java.util.ArrayList<>();

    // 360° feedback captured on the card.
    private String selfFeedback;
    private String peerFeedback;
    private String parentFeedback;

    private String goals;

    @Builder.Default
    private Boolean published = false;

    /** Attainment in one learning domain. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainAssessment {
        private String domain; // e.g. "physical", "cognitive", "language"
        private HpcLevel level;
        private String observation;
    }
}
