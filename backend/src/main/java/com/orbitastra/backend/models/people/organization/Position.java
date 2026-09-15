package com.orbitastra.backend.models.people.organization;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

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
 * School-approved job position that can be assigned through an EmploymentRecord.
 *
 * <p>{@code departmentDocsId} links to Department.id. Position hierarchy uses
 * {@code reportsToPositionDocsId}; an individual manager override can still be
 * stored in EmploymentRecord.managerDocsId. Filled headcount is calculated from
 * current employment records and is not duplicated here.
 */
@Document(collection = "staff_positions")
@CompoundIndexes({
        // NO UNIQUE INDEX HERE. positionCode was removed on 2026-09-15 and its
        // school_position_code_uniq went with it, necessarily: a unique index naming a field the
        // model no longer declares is not inert. Every document then indexes a missing value, so
        // the key is identical for all of them and the collection accepts exactly ONE row per
        // school — the school_year_class_code_uniq defect this project already shipped, found
        // live, and had to migrate 659 documents out of.
        //
        // A position is addressed by its document id, which is what EmploymentRecord stores as
        // positionDocsId.
        // Added 2026-09-15, when positionCode was removed: with the code gone nothing constrained
        // a duplicate seat, so the title carries that job within its department.
        //
        // IT DOES NOT FILTER ON active, deliberately and consistently with every other unique
        // index in this project - a retired seat keeps its title, because records made against it
        // still name it. A check that skipped retired rows would accept a write the index refuses.
        //
        // MONGO COMPARES IT CASE-SENSITIVELY. The service folds case before asking, so it is the
        // stricter of the two and the enforcement in practice: "Mathematics Teacher" and
        // "mathematics teacher" are one title to the service and two keys to the index.
        @CompoundIndex(
                name = "school_department_title_uniq",
                def = "{'schoolId': 1, 'departmentDocsId': 1, 'title': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_department_position_active_idx",
                def = "{'schoolId': 1, 'departmentDocsId': 1, 'active': 1, 'title': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Position extends SchoolBase {

    // Example: "Mathematics Teacher"
    @NotBlank
    private String title;

    // Links to Department.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String departmentDocsId;

    // Optionally links to the supervising Position.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String reportsToPositionDocsId;

    // Approved number of employees for this position. Example: 8
    @NotNull
    @Builder.Default
    private Integer approvedHeadcount = 1;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean teachingPosition = false;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
