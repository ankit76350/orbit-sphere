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

/** One section of a SchoolClass in one academic year. */
@Document(collection = "class_sections")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_class_section_code_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_class_section_active_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'active': 1, 'name': 1}"),
        @CompoundIndex(
                name = "school_section_class_teacher_idx",
                def = "{'schoolId': 1, 'classTeacherDocsId': 1, 'academicYear': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSection extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // Stable key within the class. Example: "A"
    @NotBlank
    private String sectionCode;

    // Example: "Section A - Pioneers"
    @NotBlank
    private String name;

    // Optionally links to the class teacher's Staff.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String classTeacherDocsId;

    // Maximum planned student count. Example: 40
    private Integer capacity;

    // Optionally links to a future facility/resource document.
    // Example: "67aa15d9dc3f7d0033333333"
    private String roomResourceDocsId;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
