package com.orbitastra.backend.models.academics.structure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.base.SchoolBase;

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
 * <p>Its bounded sections and subject assignments are embedded because they
 * exist only inside this class. Other documents reference the class by id and
 * an embedded section or subject by its stable code.
 */
@Document(collection = "school_classes")
@CompoundIndexes({
        // REPOINTED 2026-09-10, from 'classCode' to 'name'. It named classCode, which this
        // model never declared - so every document indexed a MISSING value, they all collided,
        // and a school could hold exactly one class per academic year. The index was live in
        // edusphere_dev and the collection was empty, so the first two classes ever created
        // would have hit it.
        //
        // 'name' rather than a new code field, because a class is addressed and referenced by
        // its document id: twelve other documents store classDocsId, and not one stores a class
        // code. sectionNo and subjectCode are codes only because they are EMBEDDED and have no
        // id to reference - a top-level document does not need one.
        //
        // Unique on 'name' does NOT make the name immutable, which it would if the name were the
        // join key. Nothing joins on it, so a rename only has to not collide.
        @CompoundIndex(
                name = "school_year_class_name_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_class_active_order_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'active': 1, 'displayOrder': 1}"),
        @CompoundIndex(
                name = "school_year_class_teacher_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'sections.classTeacherDocsId': 1}"),
        @CompoundIndex(
                name = "school_year_subject_teacher_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'subjects.teacherDocsIds': 1}"),
        @CompoundIndex(
                name = "school_year_subject_code_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'subjects.subjectCode': 1}")
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

    // Display name, editable, and unique within the year. Example: "Grade 7"
    @NotBlank
    private String name;

    // Optionally links to AffiliationProgramme.id.
    // Example: "67aa15d9dc3f7d0011111111"
    private String affiliationProgrammeDocsId;

    //! Sorting order used by the UI. Example: 7
    private Integer displayOrder;

    // Sections owned by this class. Example: [{"sectionNo": "A"}, {"sectionNo": "B"}]
    @Builder.Default
    private List<ClassSection> sections = new ArrayList<>();

    // Subject and teacher assignments owned by this class.
    // Example: [{"subjectCode": "MATHEMATICS", "name": "Mathematics", "sectionNo": "A"}]
    @Builder.Default
    private List<ClassSubject> subjects = new ArrayList<>();

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
