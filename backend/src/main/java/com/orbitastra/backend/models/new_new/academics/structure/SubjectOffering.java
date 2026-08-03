package com.orbitastra.backend.models.new_new.academics.structure;

import java.util.ArrayList;
import java.util.List;

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
 * Assignment of one Subject to a class or a specific section for one year.
 *
 * <p>This replaces subjects embedded inside SchoolClass and supports multiple
 * teachers, section-specific teaching, timetables, homework, and grading.
 */
@Document(collection = "subject_offerings")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_class_section_subject_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionDocsId': 1, 'subjectDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_teacher_offerings_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'teacherDocsIds': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_year_subject_offerings_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'subjectDocsId': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectOffering extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // Optionally links to ClassSection.id; null means class-wide.
    // Example: "67aa15d9dc3f7d0022222222"
    private String sectionDocsId;

    // Links to Subject.id. Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String subjectDocsId;

    // Links to the Staff.id values of teachers assigned to this subject.
    // Example: ["67aa15d9dc3f7d0044444444"]
    @Builder.Default
    private List<String> teacherDocsIds = new ArrayList<>();

    // Optionally links to GradingScheme.id.
    // Example: "67aa15d9dc3f7d0055555555"
    private String gradingSchemeDocsId;

    // Planned periods per week. Example: 6
    private Integer periodsPerWeek;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
