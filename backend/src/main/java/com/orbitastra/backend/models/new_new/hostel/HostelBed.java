package com.orbitastra.backend.models.new_new.hostel;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.hostel.enums.BedStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One bed in one room.
 *
 * <p>Allocation is at bed level rather than room level on purpose. A roll call and a
 * fire register both need to say exactly where a child sleeps, and "somewhere in room
 * 204" is not an answer at two in the morning.
 *
 * <p>**This model does not hold who is in the bed.** The reference sketch put a
 * {@code studentDocsId} here as well as on the stay record, which is two places holding
 * the same fact and therefore two places that can disagree. Occupancy lives on
 * HostelAllocation, and this row only says whether the bed can be given to anybody.
 *
 * <p>{@code status} is kept here rather than worked out from the allocations because a
 * warden looking for space needs it in one read, without walking every allocation in
 * the building.
 *
 * <p>The service checks that the number of beds in a room does not exceed the room's
 * capacity, that only an AVAILABLE bed is allocated, and that a bed with an ACTIVE
 * allocation is never withdrawn.
 */
@Document(collection = "hostel_beds")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_hostel_bed_uniq",
                def = "{'schoolId': 1, 'hostelRoomDocsId': 1, 'bedNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_hostel_bed_free_idx",
                def = "{'schoolId': 1, 'hostelBuildingDocsId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelBed extends SchoolBase {

    // Links to HostelBuilding.id, copied in so free beds can be found for a whole
    // building without loading every room. Example: "67ba1122dc3f7d0011223344"
    @NotBlank
    private String hostelBuildingDocsId;

    // Links to HostelRoom.id. Example: "67ba1123dc3f7d0022334455"
    @NotBlank
    private String hostelRoomDocsId;

    // Which bed in the room, as marked. Example: "204-B"
    @NotBlank
    private String bedNumber;

    // Example: BedStatus.OCCUPIED
    @NotNull
    @Builder.Default
    private BedStatus status = BedStatus.AVAILABLE;

    // When the status last changed. Example: 2026-06-15T05:00:00Z
    private Instant statusChangedAt;

    // Why, when the bed is not AVAILABLE or OCCUPIED.
    // Example: "Frame broken; carpenter coming on Monday."
    private String statusReason;

    // Example: "Near the window; kept for a child who needs the light."
    private String remarks;
}
