package com.orbitastra.backend.models.academics.enums;

/** Student participation state for one examination component. */
public enum ExamParticipationStatus {
    ATTENDED,
    ABSENT,
    EXEMPT,
    WITHHELD,

    /** Stopped from sitting this paper — debarred on the exam's candidate list when it was held. */
    DEBARRED
}
