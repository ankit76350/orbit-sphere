package com.orbitastra.backend.models.hostel;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.hostel.enums.HostelType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One hostel building.
 *
 * <p>{@code hostelType} is a rule, not a description. The service refuses an allocation
 * that would put a girl in a boys' building, and it refuses it on the building rather
 * than leaving it to whoever is filling in the form at the time.
 *
 * <p>{@code wardenStaffDocsId} is who is responsible for the children in it. A roll call
 * with nobody accountable for acting on it is a list, not a safety measure, so the
 * building always names somebody.
 *
 * <p>{@code active} being false closes a building without deleting it, so the
 * allocations and roll calls already recorded there still read correctly.
 */
@Document(collection = "hostel_buildings")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_hostel_building_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_hostel_building_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'hostelType': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelBuilding extends SchoolBase {

    // Name everybody uses. Example: "Tagore House"
    @NotBlank
    private String name;

    // Who may live here. Checked before any allocation. Example: HostelType.BOYS
    @NotNull
    private HostelType hostelType;

    // How many floors, for finding a room. Example: 3
    private Integer totalFloors;

    // Where it is on the campus. Example: "Behind the sports ground, north side."
    private String location;

    // Links to Staff.id for the warden responsible for the children here.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String wardenStaffDocsId;

    // Number families should ring at night. Example: "+912223456789"
    private String contactNumber;

    // Whether children may still be allocated here. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Example: "Ground floor rooms kept for Class VI and VII."
    private String remarks;
}
