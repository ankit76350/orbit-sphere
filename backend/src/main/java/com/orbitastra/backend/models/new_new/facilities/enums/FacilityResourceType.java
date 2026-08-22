package com.orbitastra.backend.models.new_new.facilities.enums;

/**
 * What kind of space or structure a facility resource is.
 *
 * <p>Fixed by the platform rather than typed by each school, for the usual reason: "Lab",
 * "lab" and "Laboratory" cannot be counted, and "how many classrooms does this school have"
 * has to have an answer.
 *
 * <p>BUILDING and FLOOR are in the list because the register is a tree — a room sits on a
 * floor which sits in a building — and the parent has to be a row like any other. Without
 * them, every room would have to repeat its building's name as text.
 *
 * <p>The list deliberately includes the unglamorous ones. TOILET_BLOCK, STAIRWELL and
 * CORRIDOR are where most of what a school actually gets complained about happens, and a
 * register that only holds classrooms cannot record that the second-floor toilets were
 * inspected or that the stair railing was repaired.
 */
public enum FacilityResourceType {
    /** A whole building. Usually the top of a branch. */
    BUILDING,

    /** One floor of a building. */
    FLOOR,

    /** A room lessons are taught in. */
    CLASSROOM,

    /** A science, computer or language laboratory. */
    LABORATORY,

    /** An assembly hall, auditorium or examination hall. */
    HALL,

    /** A library reading room or stack space. */
    LIBRARY_SPACE,

    /** Staff room, principal's office, accounts office. */
    OFFICE,

    /** The nurse's room. */
    CLINIC_ROOM,

    /** A block of toilets and washrooms. */
    TOILET_BLOCK,

    /** A kitchen or pantry. */
    KITCHEN,

    /** A store room or godown. */
    STORE_ROOM,

    /** An open playground or field. */
    PLAYGROUND,

    /** A marked court for a particular sport. */
    SPORTS_COURT,

    /** A staircase. */
    STAIRWELL,

    /** A corridor or passage. */
    CORRIDOR,

    /** Parking for buses or staff vehicles. */
    PARKING,

    /** Water tank, pump house, generator room, electrical panel room. */
    UTILITY,

    /** Anything the list above does not fit. */
    OTHER
}
