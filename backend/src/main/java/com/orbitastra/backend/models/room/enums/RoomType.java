package com.orbitastra.backend.models.room.enums;

/**
 * What kind of space a room is. A fixed list, so "how many labs does the school have" has one
 * answer instead of "Lab", "lab" and "Laboratory".
 */
public enum RoomType {
    /** An ordinary classroom. */
    CLASSROOM,

    /** A science or computer lab. */
    LABORATORY,

    /** The assembly or exam hall. */
    HALL,

    /** The library. */
    LIBRARY,

    /** A playground, ground or sports court. */
    PLAYGROUND,

    /** An office or staff room. */
    OFFICE,

    /** Anything else. */
    OTHER
}
