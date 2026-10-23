package com.orbitastra.backend.models.room;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.room.enums.RoomStatus;
import com.orbitastra.backend.models.room.enums.RoomType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One room or space in the school — a classroom, a lab, the hall, the playground.
 *
 * <p>It is here so a timetable period and an exam paper can say <b>where</b> they happen
 * ({@code roomDocsId}), and so two papers are not put in the same room at the same
 * time. That is all it is for; it keeps no bookings, assets or repairs.
 *
 * <p>The {@code name} is the room's only key — unique in the school, ignoring case. There is no
 * room code. A room that is no longer used is {@code CLOSED} rather than deleted, because old
 * timetables and papers still point at it.
 */
@Document(collection = "rooms")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_room_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_room_type_status_idx",
                def = "{'schoolId': 1, 'resourceType': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Room extends SchoolBase {

    // The room's name, unique in the school ignoring case. Example: "Room 12"
    @NotBlank
    private String name;

    // What kind of space it is. Example: RoomType.CLASSROOM
    @NotNull
    private RoomType resourceType;

    // Where it is, in words. Example: "Science Block, first floor"
    private String location;

    // How many students it seats. Empty when nobody has counted. Example: 40
    private Integer capacity;

    // Whether it can be used right now. Example: RoomStatus.IN_USE
    @NotNull
    @Builder.Default
    private RoomStatus status = RoomStatus.IN_USE;

    // Example: "Projector on the left wall does not work."
    private String remarks;
}
