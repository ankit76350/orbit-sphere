package com.orbitastra.backend.models.transport;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.transport.enums.TripDirection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Which vehicle and which driver are on a route, for a stretch of days.
 *
 * <p>A route never owns a bus or a driver. Buses break down, drivers take leave,
 * and a route has to keep running through both. Keeping the pairing in its own
 * record means a cover arrangement is a new row rather than an edit that wipes out
 * who was actually driving last Tuesday.
 *
 * <p>That history is the point. When a parent asks who was driving on the day
 * something happened, the answer has to still be there, and it will not be if the
 * route simply holds the current driver.
 *
 * <p>{@code direction} being null means this pairing covers both the morning and
 * the afternoon run, which is the usual case. Setting it means a different driver
 * or bus does each way.
 *
 * <p>The service checks that the vehicle and driver are both ACTIVE on the day,
 * that their papers have not run out, that no two current assignments overlap for
 * the same route and direction, and that a driver is not on two routes at the same
 * time of day.
 */
@Document(collection = "route_assignments")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_route_assignment_current_idx",
                def = "{'schoolId': 1, 'routeDocsId': 1, 'active': 1, 'effectiveFrom': -1}"),
        @CompoundIndex(
                name = "school_assignment_vehicle_idx",
                def = "{'schoolId': 1, 'vehicleDocsId': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_assignment_driver_idx",
                def = "{'schoolId': 1, 'transportDriverDocsId': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RouteAssignment extends SchoolBase {

    // Links to TransportRoute.id. Example: "67b21122dc3f7d0011223344"
    @NotBlank
    private String routeDocsId;

    // Links to TransportVehicle.id. Example: "67b21123dc3f7d0022334455"
    @NotBlank
    private String vehicleDocsId;

    // Links to TransportDriver.id. Example: "67b21124dc3f7d0033445566"
    @NotBlank
    private String transportDriverDocsId;

    // Links to Staff.id for the person who rides along and helps children on and
    // off. Null when the route runs without one.
    // Example: "67aa15d9dc3f7d0066666666"
    private String attendantStaffDocsId;

    // Which run this pairing covers. Null means both the morning and the
    // afternoon run. Example: TripDirection.PICKUP
    private TripDirection direction;

    // First day this pairing applies. Example: 2026-08-01
    @NotNull
    private LocalDate effectiveFrom;

    // Last day it applies. Null means it carries on until somebody changes it.
    // Example: 2026-08-14
    private LocalDate effectiveTo;

    // Whether this is the pairing in force now. Older rows are kept with this
    // turned off so the history stays readable. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Why this pairing was made, which matters most for a short cover.
    // Example: "Regular driver on leave until 14 August."
    private String assignmentReason;

    // Links to the staff identity that made the assignment.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String assignedByDocsId;

    // Example: "Attendant added because of the number of infants on this route."
    private String remarks;
}
