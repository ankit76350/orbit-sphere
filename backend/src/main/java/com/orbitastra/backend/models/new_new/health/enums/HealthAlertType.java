package com.orbitastra.backend.models.new_new.health.enums;

/** What kind of thing the school has to know about a child. */
public enum HealthAlertType {
    /** Reacts badly to a food, a medicine, an insect sting or similar. */
    ALLERGY,

    /** A long-term condition such as asthma, epilepsy or diabetes. */
    CHRONIC_CONDITION,

    /** Must not be given certain foods, whether for health or belief. */
    DIETARY_RESTRICTION,

    /** Cannot do some physical activity, whether for a while or for good. */
    PHYSICAL_LIMITATION,

    /** Needs medicine during the school day. */
    MEDICATION_REQUIRED,

    /** Needs help with seeing, hearing, moving or learning. */
    SUPPORT_NEED,

    /** Anything the types above do not cover. */
    OTHER
}
