package com.orbitastra.backend.dto.room.room.request;

import com.orbitastra.backend.models.room.enums.RoomStatus;
import com.orbitastra.backend.models.room.enums.RoomType;

/**
 * The filters room #2 takes, all optional and all combined with AND. Bound from the query string,
 * so a type or status that does not exist is a 400.
 */
public record RoomSearchRequest(

        /** Only rooms of this kind, such as HALL. */
        RoomType resourceType,

        /** Only rooms in this state, such as IN_USE. */
        RoomStatus status,

        /** Part of the name or the location, ignoring upper and lower case. Blank is treated as absent. */
        String search,

        /** Only rooms that seat at least this many — "which rooms can take 60 for an exam". */
        Integer minCapacity,

        Integer page,
        Integer size,
        String sort) {
}
