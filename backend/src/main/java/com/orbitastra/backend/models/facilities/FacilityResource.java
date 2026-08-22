package com.orbitastra.backend.models.new_new.facilities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.facilities.enums.FacilityResourceStatus;
import com.orbitastra.backend.models.new_new.facilities.enums.FacilityResourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One space or piece of the school's physical plant.
 *
 * <p>A building, a floor, a classroom, a laboratory, the assembly hall, the second-floor
 * toilets, the generator room. One row each, and they form a tree:
 *
 * <pre>
 * Science Block            BUILDING
 *   First Floor            FLOOR      parentResourceDocsId -> Science Block
 *     Physics Lab          LABORATORY parentResourceDocsId -> First Floor
 *     Chemistry Lab        LABORATORY
 *     Toilets (First)      TOILET_BLOCK
 * </pre>
 *
 * <p>{@code parentResourceDocsId} points at another row in this same collection, never at
 * itself. It is the same self-referencing arrangement as InventoryCategory, and it is here for
 * the same reason: a school has two or three levels of nesting and a fixed
 * building-floor-room shape would be wrong for the school that has wings, or blocks, or a
 * single-storey campus with no floors at all.
 *
 * <p>**This model does not replace HostelRoom or MessHall.** Those are owned by `hostel` and
 * `mess`, and they carry things this cannot: how many beds, whether there is air conditioning,
 * who the mess manager is. A room boarders sleep in is a residence concern that happens to be
 * in a building. Rewriting two finished packages to hang off this one would have bought
 * tidiness and cost a great deal; instead, maintenance and inspection reach them through
 * MaintenanceTargetType. The same is true of a classroom used by the timetable: nothing in
 * `academics` points here yet, and it does not have to.
 *
 * <p>{@code bookable} separates a hall anybody may ask for from an ordinary classroom, which
 * belongs to its section. It is not the same as "free": a bookable hall may also have games
 * timetabled in it, so a booking is checked against the timetable and the datesheet as well as
 * against other bookings. See ResourceBooking.
 *
 * <p>{@code accessible} is one boolean rather than a list of features, and it is deliberately
 * blunt: can a child in a wheelchair get in and use this. A school that wants to record ramps
 * and lifts and door widths separately is describing the building; this records whether the
 * answer is yes.
 *
 * <p>DECOMMISSIONED rows are never deleted. Inspections and work orders point at them, and a
 * demolished building still has a history somebody may be asked about.
 *
 * <p>The service checks that a resource is not its own ancestor, that a parent is a plausible
 * container for its child, that {@code resourceCode} is never renamed once anything points at
 * it, and that a resource with active bookings or open work orders is not decommissioned
 * without those being dealt with.
 */
@Document(collection = "facility_resources")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_facility_code_uniq",
                def = "{'schoolId': 1, 'resourceCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_facility_parent_idx",
                def = "{'schoolId': 1, 'parentResourceDocsId': 1, 'sortOrder': 1}"),
        @CompoundIndex(
                name = "school_facility_type_status_idx",
                def = "{'schoolId': 1, 'resourceType': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_facility_bookable_idx",
                def = "{'schoolId': 1, 'bookable': 1, 'status': 1, 'capacity': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResource extends SchoolBase {

    // The school's own code, which is what gets painted on a door or written on a
    // timetable. Never renamed once anything points at it. Example: "SCI-1-PHY-LAB"
    @NotBlank
    private String resourceCode;

    // What people call it. Example: "Physics Laboratory"
    @NotBlank
    private String name;

    // What kind of space this is. Example: FacilityResourceType.LABORATORY
    @NotNull
    private FacilityResourceType resourceType;

    // Links to FacilityResource.id of the space this one sits inside — a room's floor, a
    // floor's building. Another row in this same collection, never this row itself. Null
    // for the top of a branch. Example: "67c31122dc3f7d0011223344"
    private String parentResourceDocsId;

    // How to find it, for somebody who does not know the building.
    // Example: "First floor, Science Block, at the far end past the staff room"
    private String locationDescription;

    // How many people it holds. Null for a space where the question makes no sense, such
    // as a corridor. Example: 36
    private Integer capacity;

    // Whether somebody may ask to use this for an event. False for an ordinary classroom,
    // which belongs to its section; true for the assembly hall.
    //
    // True does not mean free: the hall also has games timetabled in it on Tuesdays. A
    // booking has to be checked against DailyTimetable.entries and ExamSchedule as well as
    // against other bookings. Example: false
    @NotNull
    @Builder.Default
    private Boolean bookable = false;

    // Whether a child in a wheelchair can get in and use it. One blunt question rather
    // than a list of building features. Example: true
    @NotNull
    @Builder.Default
    private Boolean accessible = false;

    // Whether it can be used at all, and if not, whether that is temporary.
    // Example: FacilityResourceStatus.IN_USE
    @NotNull
    @Builder.Default
    private FacilityResourceStatus status = FacilityResourceStatus.IN_USE;

    // Why it is closed or decommissioned. Required for both, because a room nobody may
    // use with no reason written down gets reopened by the next person who needs one.
    // Example: "Roof leak over the back benches. Closed until the monsoon repair."
    private String statusReason;

    // Links to Staff.id of whoever is answerable for this space — a lab in charge, a
    // hall coordinator. Example: "67aa15d9dc3f7d0044444444"
    private String inChargeStaffDocsId;

    // Links to Department.id when a space belongs to one. Example: "67aa2211dc3f7d0011223344"
    private String departmentDocsId;

    // Which floor this is on, as a number, for a space inside a building. Kept as well as
    // the parent link because "everything on the second floor" is a question people ask
    // and walking the tree to answer it is silly. Ground is 0. Example: 1
    private Integer floorNumber;

    // Floor area in square feet, where the school knows it. Used for cleaning contracts
    // and occupancy limits. Example: 640.00
    private Double areaSquareFeet;

    // Where this appears in a list of its siblings. Example: 10
    @Builder.Default
    private Integer sortOrder = 0;

    // Links to DocumentRecord.id for a floor plan, a photograph, an occupancy
    // certificate. Example: ["67c31123dc3f7d0022334455"]
    @Builder.Default
    private List<String> documentDocsIds = new ArrayList<>();

    // Anything worth knowing.
    // Example: "Gas line runs along the back wall. Isolate before any drilling."
    private String remarks;
}
