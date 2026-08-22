package com.orbitastra.backend.models.new_new.health.enums;

/**
 * How much the school knows a vaccination really happened.
 *
 * <p>Kept apart from the record itself because a parent saying a jab was given and
 * a certificate proving it are not the same thing, and a board asking for proof
 * will not accept the first.
 */
public enum ImmunizationVerificationStatus {
    /** A parent told us, with nothing to show for it. */
    PARENT_REPORTED,

    /** A certificate or card has been uploaded but nobody has checked it. */
    EVIDENCE_UPLOADED,

    /** Somebody at the school has looked at the evidence and accepts it. */
    VERIFIED,

    /** The evidence did not match what was claimed. */
    DISPUTED
}
