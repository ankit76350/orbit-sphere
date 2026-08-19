package com.orbitastra.backend.models.new_new.hostel;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.hostel.enums.RoomType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One room in a hostel building.
 *
 * <p>A room holds beds and nothing else. Who sleeps in it is answered by the beds and
 * the allocations against them, not here, because a room that also tracked its
 * occupants would end up disagreeing with them.
 *
 * <p>{@code capacity} is how many beds the room is built for. The service uses it to
 * refuse a fourth bed being added to a three-bed room, which is how a warden discovers
 * a fire limit has been quietly exceeded.
 */
@Document(collection = "hostel_rooms")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_hostel_room_uniq",
                def = "{'schoolId': 1, 'hostelBuildingDocsId': 1, 'roomNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_hostel_room_building_idx",
                def = "{'schoolId': 1, 'hostelBuildingDocsId': 1, 'floorNumber': 1, 'roomNumber': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelRoom extends SchoolBase {

    // Links to HostelBuilding.id. Example: "67ba1122dc3f7d0011223344"
    @NotBlank
    private String hostelBuildingDocsId;

    // Room number as painted on the door. Example: "204"
    @NotBlank
    private String roomNumber;

    // Which floor it is on. Example: 2
    private Integer floorNumber;

    // Example: RoomType.TRIPLE
    @NotNull
    private RoomType roomType;

    // How many beds the room is built for. Adding more than this is refused.
    // Example: 3
    @NotNull
    @Positive
    private Integer capacity;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean airConditioned = false;

    // Whether children may still be allocated here. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Example: "Window latch broken, reported 12 August."
    private String remarks;
}
