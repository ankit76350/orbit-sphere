package com.orbitastra.backend.models.new_new.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Common fields for top-level documents that belong to both a student and an
 * academic year. The fields are inherited so every collection keeps the same
 * MongoDB property names and indexes.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcadmicStudentSchoolBase extends SchoolBase{

    //this is the academic year name and the acadmic year name will be imuttable we can't be able to edit the academic year name after created
    @Indexed
    private String academicYear;

    @Indexed
    private String studentDocsId;
}
