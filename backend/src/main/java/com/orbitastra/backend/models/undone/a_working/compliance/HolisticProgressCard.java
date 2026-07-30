package com.orbitastra.backend.models.undone.a_working.compliance;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.undone.a_working.compliance.enums.HpcLevel;
import com.orbitastra.backend.models.undone.a_working.compliance.enums.LearningDomain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * NEP 2020 Holistic Progress Card (HPC).
 *
 * A comprehensive 360° assessment of a student's overall development that
 * complements traditional academic results. The Holistic Progress Card
 * evaluates multiple learning domains, captures teacher observations, and
 * includes self, peer, and parent feedback to support continuous development
 * in accordance with the National Education Policy (NEP) 2020.
 */
@Document(collection = "holistic_progress_cards")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HolisticProgressCard extends AcademicStudentSchoolBase {


    /** Assessment across different learning domains. */
    @Builder.Default
    private List<DomainAssessment> domains = new ArrayList<>();

    /** Teacher's overall remarks about the student's progress. */
    private String teacherRemarks;

    /** Student's self-reflection. */
    private String selfFeedback;

    /** Feedback received from peers. */
    private String peerFeedback;

    /** Feedback provided by the parent or guardian. */
    private String parentFeedback;

    /** Development goals for the next assessment period. */
    private String developmentGoals;

    /** Whether the progress card has been published to parents/students. */
    @Builder.Default
    private boolean published = false;

    /**
     * Assessment of a single learning domain.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainAssessment {

        /** Learning domain being assessed. */
        private LearningDomain domain;

        /** Student's attainment level in the domain. */
        private HpcLevel level;

        /** Teacher's observation for this domain. */
        private String observation;
    }
}
