package com.orbitastra.backend.models.new_new.people.staff;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.staff.enums.EmploymentStatus;
import com.orbitastra.backend.models.new_new.people.staff.enums.EmploymentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One period of employment and position history for a Staff member.
 *
 * <p>A staff profile remains stable while employment records can be closed and
 * recreated for promotions, transfers, separation, or rejoining. At most one
 * record per staff member may have {@code current = true}.
 */
@Document(collection = "employment_records")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_staff_employment_start_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'effectiveFrom': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_staff_current_employment_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'current': 1}",
                unique = true,
                partialFilter = "{'current': true}"),
        @CompoundIndex(
                name = "school_employment_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'effectiveFrom': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentRecord extends SchoolBase {

    // Links to Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Links to Position.id.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String positionDocsId;

    // Links to the supervisor's Staff.id.
    // Example: "67aa15d9dc3f7d0033333333"
    private String managerDocsId;

    // Example: EmploymentStatus.ACTIVE
    @NotNull
    private EmploymentStatus status;

    // Example: EmploymentType.FULL_TIME
    @NotNull
    private EmploymentType employmentType;

    // Joining date or beginning of this employment period. Example: 2026-04-01
    @NotNull
    private LocalDate effectiveFrom;

    // Null while this employment record remains current. Example: 2030-03-31
    private LocalDate effectiveUntil;

    // Example: 2026-09-30
    private LocalDate probationUntil;

    // True only for the staff member's current employment record.
    @NotNull
    @Builder.Default
    private Boolean current = true;

    // Example: "Voluntary resignation"
    private String separationReason;
}
