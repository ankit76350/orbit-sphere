package com.orbitastra.backend.models.new_new.conduct.enums;

/**
 * What the school decided to do about a case.
 *
 * <p>Ordered roughly from lightest to heaviest. SUSPENSION and EXPULSION are not
 * ordinary actions: they stop a child being educated, and a school will be asked to
 * justify them, which is why an action carries who approved it.
 */
public enum ConductActionType {
    /** A word from a teacher. Recorded, nothing more. */
    VERBAL_WARNING,

    /** A written warning kept on file. */
    WRITTEN_WARNING,

    /** Kept back after school or through a break. */
    DETENTION,

    /** The family comes in to talk about it. */
    PARENT_MEETING,

    /** Made to put right what they damaged, or to pay for it. */
    RESTITUTION,

    /** Loses a privilege such as a trip, a match or a post of responsibility. */
    PRIVILEGE_WITHDRAWN,

    /** Work for the school community. */
    COMMUNITY_SERVICE,

    /** Sent to the counsellor rather than punished. */
    COUNSELLING_REFERRAL,

    /** Kept out of school for a stated number of days. */
    SUSPENSION,

    /** Removed from the school for good. */
    EXPULSION,

    /** Anything the types above do not cover. */
    OTHER
}
