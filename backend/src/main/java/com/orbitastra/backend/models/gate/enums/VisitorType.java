package com.orbitastra.backend.models.gate.enums;

/** Why somebody from outside is at the school. */
public enum VisitorType {
    /** A parent or guardian of a student here. */
    PARENT,

    /** Somebody delivering or servicing something the school pays for. */
    VENDOR,

    /** A builder, painter, electrician or similar, usually here for days. */
    CONTRACTOR,

    /** Somebody invited, such as a speaker or a chief guest. */
    GUEST,

    /** A former student. */
    ALUMNI,

    /** An inspector or officer from a board or a government office. */
    GOVERNMENT,

    /** Dropping off or collecting a parcel, in and out in minutes. */
    COURIER,

    /** Here for a job interview. */
    CANDIDATE,

    /** Anybody the types above do not cover. */
    OTHER
}
