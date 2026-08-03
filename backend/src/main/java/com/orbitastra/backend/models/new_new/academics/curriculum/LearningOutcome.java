package com.orbitastra.backend.models.new_new.academics.curriculum;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** A measurable skill, competency, or knowledge outcome in a curriculum. */
@Document(collection = "learning_outcomes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_framework_outcome_code_uniq",
                def = "{'schoolId': 1, 'curriculumFrameworkDocsId': 1, 'outcomeCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_outcome_subject_class_order_idx",
                def = "{'schoolId': 1, 'curriculumFrameworkDocsId': 1, 'subjectCode': 1, 'classCode': 1, 'sequence': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningOutcome extends SchoolBase {

    // Links to CurriculumFramework.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String curriculumFrameworkDocsId;

    // Stable code supplied by the board or school. Example: "MATH-G7-ALG-01"
    @NotBlank
    private String outcomeCode;

    // Example: "Solve linear equations in one variable"
    @NotBlank
    private String title;

    // Example: "The learner solves and verifies one-variable linear equations."
    private String description;

    // References an embedded SchoolClass.subjects[].subjectCode.
    // Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Stable SchoolClass.classCode, not a year-specific class id. Example: "GRADE_7"
    private String classCode;

    // Optionally links to the parent LearningOutcome.id.
    // Example: "67aa15d9dc3f7d0033333333"
    private String parentOutcomeDocsId;

    // Example: "Applying"
    private String cognitiveLevel;

    // Display order. Example: 10
    private Integer sequence;

    // Example: ["PROBLEM_SOLVING", "NUMERACY"]
    @Builder.Default
    private List<String> competencyTags = new ArrayList<>();
}
