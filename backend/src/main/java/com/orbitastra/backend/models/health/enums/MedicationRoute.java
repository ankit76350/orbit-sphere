package com.orbitastra.backend.models.health.enums;

/** How a medicine was given. */
public enum MedicationRoute {
    /** Swallowed. */
    ORAL,

    /** Put on the skin. */
    TOPICAL,

    /** Breathed in, such as an asthma inhaler. */
    INHALED,

    /** Injected, such as an adrenaline pen. */
    INJECTION,

    /** Into the eye. */
    EYE_DROPS,

    /** Into the ear. */
    EAR_DROPS,

    /** Anything the routes above do not cover. */
    OTHER
}
