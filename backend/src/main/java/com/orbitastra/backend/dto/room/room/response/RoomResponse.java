package com.orbitastra.backend.dto.room.room.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.room.Room;
import com.orbitastra.backend.models.room.enums.RoomStatus;
import com.orbitastra.backend.models.room.enums.RoomType;

/**
 * One room of the school.
 */
public record RoomResponse(

        String roomDocsId,

        String name,

        RoomType resourceType,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String location,

        /** How many it seats. Absent when nobody has counted. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer capacity,

        RoomStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String remarks,

        Instant createdAt,

        Instant updatedAt,

        /** For the next write to this room. */
        Long version) {

    /** Builds the answer from a saved room. */
    public static RoomResponse of(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getResourceType(),
                room.getLocation(),
                room.getCapacity(),
                room.getStatus(),
                room.getRemarks(),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                room.getVersion());
    }
}
