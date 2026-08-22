package com.orbitastra.backend.models.new_new.facilities.enums;

/**
 * What is being checked on an inspection round.
 *
 * <p>Most of these are **not the school's own idea.** A fire safety certificate, a lift
 * certificate and a water test are things an authority requires, on a cycle, with a piece of
 * paper at the end that expires. That is why FacilityInspection carries a validity date and a
 * certificate document, and why the type is a fixed list: "how are we placed on fire safety"
 * has to be answerable in one query.
 *
 * <p>PLAYGROUND_EQUIPMENT is in the list because it is the one people forget. A swing bracket
 * that has rusted through is the sort of thing nobody inspects until after it fails, and a
 * school that inspects its wiring on a schedule and its climbing frame never has the balance
 * wrong.
 */
public enum InspectionType {
    /** Extinguishers, alarms, exits, drills. Usually certificated and dated. */
    FIRE_SAFETY,

    /** Structural condition: walls, roof, railings, staircases. */
    BUILDING_SAFETY,

    /** Wiring, panels, earthing, the generator. */
    ELECTRICAL,

    /** Drinking water tested for what is in it. */
    WATER_QUALITY,

    /** Kitchen, dining hall and toilet cleanliness. */
    HYGIENE,

    /** Lifts, hoists and their certificates. */
    LIFT,

    /** Swings, frames, goalposts, matting. */
    PLAYGROUND_EQUIPMENT,

    /** Laboratory safety: chemicals, gas, fume cupboards, eyewash. */
    LABORATORY_SAFETY,

    /** A general walk-round with no single subject. */
    GENERAL
}
