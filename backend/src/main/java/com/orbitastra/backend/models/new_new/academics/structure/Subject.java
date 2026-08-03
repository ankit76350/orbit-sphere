package com.orbitastra.backend.models.new_new.academics.structure;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.SubjectType;
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
 * School-owned subject master reused across academic years and classes.
 *
 * <p>A subject is assigned to a class or section through SubjectOffering.
 */
@Document(collection = "academic_subjects")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_subject_code_uniq",
                def = "{'schoolId': 1, 'subjectCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_subject_active_name_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends SchoolBase {

    // Stable school-scoped key. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Example: "Mathematics"
    @NotBlank
    private String name;

    // Example: "Maths"
    private String shortName;

    // Example: SubjectType.CORE
    @NotNull
    private SubjectType subjectType;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
