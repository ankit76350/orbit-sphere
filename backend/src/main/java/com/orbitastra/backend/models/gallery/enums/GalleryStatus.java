package com.orbitastra.backend.models.new_new.gallery.enums;

/**
 * How far an album or one piece of media has got.
 *
 * <p>WITHDRAWN is the state that makes this package safe. A family withdrawing consent for
 * photographs in November is entitled to have their child taken out of what is on display,
 * and there has to be a state that says "this was published and has been taken down" rather
 * than the row quietly disappearing. A deleted row cannot answer why something vanished, and
 * somebody will ask.
 *
 * <p>PENDING_APPROVAL exists because photographs of children should not go up because one
 * person uploaded them. Somebody who did not take the pictures ought to look before a family
 * sees them.
 */
public enum GalleryStatus {
    /** Uploaded, nobody has looked at it. Not visible to families. */
    DRAFT,

    /** Waiting for somebody other than the uploader to approve it. */
    PENDING_APPROVAL,

    /** Visible, within whatever the visibility allows. */
    PUBLISHED,

    /** Taken down after being published, with a reason recorded. */
    WITHDRAWN,

    /** Kept for the record but no longer shown. */
    ARCHIVED
}
