package com.orbitastra.backend.models.room.enums;

/**
 * Whether a room can be used. Only an {@code IN_USE} room can be given to a timetable period or
 * an exam paper.
 */
public enum RoomStatus {
    /** Open and usable. */
    IN_USE,

    /** Shut for a while — repairs, painting. It opens again. */
    UNDER_MAINTENANCE,

    /** Not used any more. Kept, because old timetables and papers point at it. */
    CLOSED
}
