package com.orbitastra.backend.models.new_new.facilities.enums;

/**
 * Who is answerable for one tagged object.
 *
 * <p>Three kinds, because "who is responsible for this projector" has three honest answers in
 * a school and only one of them is a person. A laptop is one member of staff's. A set of lab
 * apparatus belongs to the science department, and holding one teacher answerable for it when
 * four of them use it is how nobody is. A ceiling fan belongs to the room it is in.
 *
 * <p>The type is stored beside the id because nothing else on an asset row says which
 * collection the custodian is in — the same reason MaintenanceTargetType exists.
 */
public enum AssetCustodianType {
    /** One named member of staff. Points at Staff.id. */
    STAFF,

    /** A department, when several people share it. Points at Department.id. */
    DEPARTMENT,

    /** Fixed to a place rather than held by anybody. Points at FacilityResource.id. */
    FACILITY_RESOURCE
}
