package com.orbitastra.backend.models.new_new.conduct.enums;

/** What sort of thing happened. */
public enum ConductEventType {
    /** A physical fight or assault. */
    PHYSICAL_ALTERCATION,

    /** Repeated targeting of one child by others. */
    BULLYING,

    /** Name-calling, threats or abusive language. */
    VERBAL_ABUSE,

    /** Breaking or defacing school or another child's property. */
    PROPERTY_DAMAGE,

    /** Taking something that belongs to somebody else. */
    THEFT,

    /** Copying, using notes or helping somebody else cheat in an exam. */
    EXAM_MALPRACTICE,

    /** Talking, refusing to work or stopping a lesson from running. */
    CLASSROOM_DISRUPTION,

    /** Not in uniform, or not turned out as the school requires. */
    UNIFORM_BREACH,

    /** Late to school or to lessons, repeatedly. */
    PERSISTENT_LATENESS,

    /** Leaving the school or a lesson without permission. */
    ABSCONDING,

    /** Bringing something in that is not allowed. */
    PROHIBITED_ITEM,

    /** Anything the types above do not cover; the description says what. */
    OTHER
}
