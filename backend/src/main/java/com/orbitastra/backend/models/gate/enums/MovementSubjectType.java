package com.orbitastra.backend.models.new_new.gate.enums;

/**
 * Who or what went through the gate.
 *
 * <p>This says which collection {@code GateMovement.subjectDocsId} points at, so
 * one log can cover everybody instead of needing a separate log per kind of
 * person.
 */
public enum MovementSubjectType {
    /** A student. Points at Student.id. */
    STUDENT,

    /** A member of staff. Points at Staff.id. */
    STAFF,

    /** Somebody visiting. Points at Visitor.id. */
    VISITOR,

    /** A school vehicle. Points at TransportVehicle.id. */
    VEHICLE
}
