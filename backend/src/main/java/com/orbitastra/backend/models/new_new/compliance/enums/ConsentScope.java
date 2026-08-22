package com.orbitastra.backend.models.new_new.compliance.enums;

/**
 * Whether a consent stands for everything of its kind, or was given for one occasion.
 *
 * <p>Both exist because a school genuinely asks both kinds of question, and one shape cannot
 * hold them. "May the nurse give your child paracetamol when she needs it?" is asked once and
 * answered for the year. "May we give your child this antibiotic, three times a day, for the
 * next five days?" is asked about one course of one medicine.
 *
 * <p>This is the field that decides whether the one-per-student-per-purpose unique index
 * applies. Without it, either a family can only ever give one MEDICAL_TREATMENT consent in
 * their child's whole time at the school, or the index has to be dropped and a school ends up
 * with four contradictory standing consents for photographs and no way to say which is
 * current.
 *
 * <p>It cannot be worked out from the consent itself. A record-specific consent is pointed at
 * by the record it was given for, and nothing on the consent row would otherwise say so — so
 * this is stored rather than derived.
 */
public enum ConsentScope {
    /**
     * Stands until withdrawn or expired, for everything of this purpose. One per student per
     * purpose at a time.
     */
    STANDING,

    /**
     * Given for one occasion, and pointed at by the record it was given for: one course of
     * medicine, one photograph, one support plan, one year in the hostel. A student may have
     * many of these for the same purpose.
     */
    RECORD_SPECIFIC
}
