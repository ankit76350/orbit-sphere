package com.orbitastra.backend.models.new_new.compliance.enums;

/**
 * How the family gave or refused consent.
 *
 * <p>Kept because the answers carry different weight if anybody argues. A signed paper form
 * is the strongest, a tick in the parent app is good, and a verbal yes noted by a member of
 * staff is the weakest. When a family says they never agreed to something, this is what
 * decides whether the school has anything to show.
 */
public enum ConsentChannel {
    /** A signed paper form, scanned and kept. The strongest evidence. */
    SIGNED_FORM,

    /** Given in the parent app. */
    PARENT_APP,

    /** Given on the school's website. */
    WEB_PORTAL,

    /** Replied to by email. */
    EMAIL,

    /** Replied to by text message. */
    SMS,

    /** Said in person or on the phone and written down by staff. The weakest. */
    VERBAL_RECORDED
}
