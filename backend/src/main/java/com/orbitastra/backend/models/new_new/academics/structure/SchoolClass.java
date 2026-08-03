package com.orbitastra.backend.models.new_new.academics.structure;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One grade or class configured for one academic year.
 *
 * <p>Sections, subjects, students, and timetable entries reference this
 * document. The academic year is stored by AcademicYear.name.
 */
@Document(collection = "school_classes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_class_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'classCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_class_active_order_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'active': 1, 'displayOrder': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Stable key within the year. Example: "GRADE_7"
    @NotBlank
    private String classCode;

    // Example: "Grade 7"
    @NotBlank
    private String name;

    // Optionally links to AffiliationProgramme.id.
    // Example: "67aa15d9dc3f7d0011111111"
    private String affiliationProgrammeDocsId;

    // Optionally links to CurriculumFramework.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String curriculumFrameworkDocsId;

    // Sorting order used by the UI. Example: 7
    private Integer displayOrder;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
