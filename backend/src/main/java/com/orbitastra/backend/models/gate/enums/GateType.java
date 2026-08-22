package com.orbitastra.backend.models.gate.enums;

/** What a gate is mainly used for. */
public enum GateType {
    /** The main gate, used by everybody. */
    MAIN,

    /** A gate for people on foot only. */
    PEDESTRIAN,

    /** A gate buses and cars come through. */
    VEHICLE,

    /** A back gate for deliveries, kitchen supplies and contractors. */
    SERVICE,

    /** A gate kept shut except in an emergency. */
    EMERGENCY_EXIT
}
