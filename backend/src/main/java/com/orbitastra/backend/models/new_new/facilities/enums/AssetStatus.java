package com.orbitastra.backend.models.new_new.facilities.enums;

/**
 * Where one tagged object stands.
 *
 * <p>LOST and DISPOSED are deliberately different, and the difference is the point of having
 * this field at all. Disposed is the school deciding to get rid of something and recording
 * what it got for it. **Lost is nobody knowing where it went**, which is the state a school
 * would rather not have a name for and the one an auditor asks about first.
 *
 * <p>A register where the only ways out are "disposed" and "written off" quietly turns every
 * missing microscope into a disposal, and then nobody can count how many things the school
 * cannot find. This is the same reason `inventory` keeps WASTAGE apart from
 * ADJUSTMENT_DECREASE.
 *
 * <p>WRITTEN_OFF is the accounting end: the thing may still be sitting there, but the school
 * no longer carries it as worth anything. A broken projector nobody has removed from the
 * ceiling is written off and still physically present.
 */
public enum AssetStatus {
    /** Being used, by somebody or somewhere. */
    IN_USE,

    /** Held in a store, working, not currently issued. */
    IN_STORE,

    /** Away being repaired, or waiting for a repair. */
    UNDER_REPAIR,

    /** Nobody knows where it is. */
    LOST,

    /** Deliberately got rid of, with what was received for it recorded. */
    DISPOSED,

    /** No longer carried as having value, whether or not it is still physically there. */
    WRITTEN_OFF
}
