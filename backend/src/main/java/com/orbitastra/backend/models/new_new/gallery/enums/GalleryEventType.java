package com.orbitastra.backend.models.new_new.gallery.enums;

/**
 * What the album is of.
 *
 * <p>Used for grouping and for the filter parents actually use, which is "show me sports day".
 * The list is the occasions an Indian school photographs year after year; anything unusual goes
 * under OTHER with the album title saying what it was.
 */
public enum GalleryEventType {
    /** The school's annual day. */
    ANNUAL_DAY,

    /** Sports day or the annual athletic meet. */
    SPORTS_DAY,

    /** A national day: Republic Day, Independence Day. */
    NATIONAL_DAY,

    /** A festival celebrated at school. */
    FESTIVAL,

    /** Teachers' Day or Children's Day. */
    SPECIAL_DAY,

    /** A science, art or book exhibition. */
    EXHIBITION,

    /** A concert, play or dance programme. */
    CULTURAL_PROGRAMME,

    /** An inter-school or intra-school competition. */
    COMPETITION,

    /** A picnic or an educational trip. */
    TRIP,

    /** Graduation, farewell or a leaving ceremony. */
    FAREWELL,

    /** A visiting speaker, chief guest or workshop. */
    GUEST_VISIT,

    /** Ordinary school life, with no particular occasion. */
    EVERYDAY_LIFE,

    /** Something the list above does not cover; the title says what. */
    OTHER
}
