package com.orbitastra.backend.models.facilities.enums;

/**
 * What a maintenance job, plan or inspection is about.
 *
 * <p>This enum is the reason this package could be built without rewriting two others. A
 * hostel room, a mess hall, a school bus and a generator all need servicing and inspecting,
 * and they already live in three different packages that were designed before this one. The
 * alternatives were both bad: force every physical thing in the school to become a
 * FacilityResource, which means refactoring `hostel`, `mess` and `transport`; or give each of
 * those packages its own little work-order model, which means four inspection systems that
 * cannot produce one list of what is overdue.
 *
 * <p>So a work order names its target by type and id, and the packages that already own those
 * records are left alone. This is the FeeInvoice.sourceType pattern: nothing else on the row
 * knows what the target is, so the type is stored beside the id. Compare
 * FeedbackSubmission, where the topic already declared the type and storing it again would
 * have been a second fact able to disagree.
 *
 * <p>VEHICLE is what finally answers the note left in `transport/README.md`, where fuel,
 * odometer and servicing were deferred to "a facilities or maintenance module". HOSTEL_ROOM
 * and MESS_HALL answer the same note in `hostel/README.md` about inspection rounds.
 */
public enum MaintenanceTargetType {
    /** A space or a piece of the building. Points at FacilityResource.id. */
    FACILITY_RESOURCE,

    /** One individually tagged object. Points at AssetRegisterItem.id. */
    ASSET,

    /** A room boarders sleep in. Points at HostelRoom.id, owned by `hostel`. */
    HOSTEL_ROOM,

    /** A dining hall. Points at MessHall.id, owned by `mess`. */
    MESS_HALL,

    /** A bus, van or car. Points at TransportVehicle.id, owned by `transport`. */
    VEHICLE
}
