package com.orbitastra.backend.dto.room.room.request;

import com.orbitastra.backend.models.room.enums.RoomType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What room #1 needs to add a room. A new room is always {@code IN_USE}.
 */
public record RoomCreateRequest(

        /** "Room 12", "Main Hall". Unique in the school, ignoring upper and lower case. */
        @NotBlank @Size(max = 120) String name,

        /** CLASSROOM, LABORATORY, HALL, LIBRARY, PLAYGROUND, OFFICE or OTHER. */
        @NotNull RoomType resourceType,

        /** Where it is, in words. Optional. "Science Block, first floor". */
        @Size(max = 200) String location,

        /** How many students it seats. Optional; at least 1 when sent. Exam papers are checked against it. */
        @Min(1) Integer capacity,

        /** Optional notes. */
        @Size(max = 500) String remarks) {
}
