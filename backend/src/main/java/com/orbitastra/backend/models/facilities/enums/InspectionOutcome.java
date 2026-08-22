package com.orbitastra.backend.models.facilities.enums;

/**
 * What an inspection concluded.
 *
 * <p>PASSED_WITH_OBSERVATIONS exists because it is the commonest real answer and the two-value
 * version loses it. An inspector who finds three small things and signs the certificate anyway
 * has not said the building is perfect, and recording that as PASSED throws away the three
 * things. Recording it as FAILED is wrong too, and would close a school that is fine.
 *
 * <p>So the middle value is where most rounds land, and the findings beside it are what
 * somebody is meant to act on. An inspection that passes with observations and generates no
 * work orders is a round nobody read.
 */
public enum InspectionOutcome {
    /** Nothing found. */
    PASSED,

    /** Signed off, with things noted that somebody should deal with. */
    PASSED_WITH_OBSERVATIONS,

    /** Not acceptable. Something has to change before this is used again. */
    FAILED
}
