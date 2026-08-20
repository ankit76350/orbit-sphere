package com.orbitastra.backend.models.new_new.gallery.enums;

/**
 * Who may see an album.
 *
 * <p>This is the most consequential field in the package, because it decides what consent is
 * needed. A photograph shown to the child's own parents and the same photograph on the
 * school's public website are not the same act, however identical the file.
 *
 * <p>PUBLIC is the one to be careful with. Once a picture of a child is on an open web page it
 * can be copied, indexed and kept by anybody, and withdrawing it later does not undo that. So
 * every child identifiable in a PUBLIC album needs a granted PHOTOGRAPH_AND_MEDIA consent,
 * checked at the moment of publishing and not assumed from admission paperwork.
 */
public enum GalleryVisibility {
    /** Anybody on the internet. Needs consent for every child who can be identified. */
    PUBLIC,

    /** Signed-in parents of the school. Still needs consent, but the risk is contained. */
    PARENTS,

    /** Staff only. Useful for photographs taken before anybody has decided anything. */
    STAFF,

    /** Only the people named on the album. Effectively unpublished. */
    PRIVATE
}
