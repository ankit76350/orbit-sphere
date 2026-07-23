package com.orbitastra.backend.models.base;

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
public class AcadmicStudentBaseDocs extends BaseDocument{

    @Indexed
    private String academicYear;

    @Indexed
    private String studentId;
}
