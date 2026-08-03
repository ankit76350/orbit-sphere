package com.orbitastra.backend.models.new_new.academics.curriculum;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.CurriculumStatus;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One teachable unit in a year-specific SubjectOffering. */
@Document(collection = "curriculum_units")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_offering_unit_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'subjectOfferingDocsId': 1, 'unitCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_offering_unit_order_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'subjectOfferingDocsId': 1, 'status': 1, 'sequence': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumUnit extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to CurriculumFramework.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String curriculumFrameworkDocsId;

    // Links to SubjectOffering.id.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String subjectOfferingDocsId;

    // Stable code within the offering. Example: "ALGEBRA_01"
    @NotBlank
    private String unitCode;

    // Example: "Algebraic expressions and identities"
    @NotBlank
    private String title;

    // Teaching order. Example: 1
    private Integer sequence;

    // Planned lessons. Example: 12
    private Integer plannedPeriods;

    // Example: CurriculumStatus.APPROVED
    @NotNull
    @Builder.Default
    private CurriculumStatus status = CurriculumStatus.DRAFT;

    // Example: 2026-04-15T10:30:00Z
    private Instant approvedAt;

    // Links to the approving Staff.id.
    // Example: "67aa15d9dc3f7d0033333333"
    private String approvedByDocsId;

    // Example: "Use practical examples before symbolic exercises."
    private String teachingNotes;

    // Links to LearningOutcome.id values covered by this unit.
    // Example: ["67aa15d9dc3f7d0044444444"]
    @Builder.Default
    private List<String> learningOutcomeDocsIds = new ArrayList<>();

    // Links to CurriculumUnit.id values that should be completed first.
    // Example: ["67aa15d9dc3f7d0055555555"]
    @Builder.Default
    private List<String> prerequisiteUnitDocsIds = new ArrayList<>();
}
