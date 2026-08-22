package com.orbitastra.backend.models.new_new.transport;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.transport.enums.TripDirection;
import com.orbitastra.backend.models.new_new.transport.enums.TripStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One run of one route on one day, in one direction.
 *
 * <p>A route is a plan. A trip is what actually happened on Tuesday morning. The
 * two are kept apart because the plan barely changes and the runs happen twice a
 * day, every school day, and each one has its own story: who drove, which bus,
 * when it really left, who was on it.
 *
 * <p>Morning and afternoon are two separate trips, not one. They can have
 * different drivers, different buses and different children, so treating them as
 * one run would lose all three.
 *
 * <p>The driver, vehicle and attendant are copied onto the trip when it is made,
 * rather than being read through the route assignment every time. A trip has to
 * keep showing who was actually driving that day even after the assignment is
 * changed next week. The same reason an invoice line keeps its own copy of the
 * fee head name.
 *
 * <p>There is no trip number from NumberSequence. A trip is already identified by
 * its route, its date and its direction, and those three are unique together. A
 * daily run of hundreds of trips does not need to queue for numbers as well.
 *
 * <p>{@code expectedStudentCount} is filled in when the trip list is made, from
 * the allocations that apply that day. Comparing it with
 * {@code boardedStudentCount} at the end is how the school notices a child who
 * never got on.
 *
 * <p>This model carries no live position. Where the bus is right now is not stored
 * here. See the README for why.
 *
 * <p>The service checks that the vehicle and driver are fit to run that day, that
 * a trip is not started on a non-working day unless the school says otherwise, and
 * that a cancelled trip carries a reason.
 */
@Document(collection = "transport_trips")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_route_date_direction_uniq",
                def = "{'schoolId': 1, 'routeDocsId': 1, 'serviceDate': 1, 'direction': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_trip_date_status_idx",
                def = "{'schoolId': 1, 'serviceDate': -1, 'status': 1}"),
        @CompoundIndex(
                name = "school_year_trip_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'serviceDate': -1}"),
        @CompoundIndex(
                name = "school_trip_vehicle_idx",
                def = "{'schoolId': 1, 'vehicleDocsId': 1, 'serviceDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportTrip extends SchoolBase {

    // Links to AcademicYear.name. Example: "2026-2027"
    @Indexed
    @NotBlank
    private String academicYear;

    // The day this run happened. Example: 2026-08-18
    @NotNull
    private LocalDate serviceDate;

    // Links to TransportRoute.id. Example: "67b21122dc3f7d0011223344"
    @NotBlank
    private String routeDocsId;

    // Which way this run goes. Example: TripDirection.PICKUP
    @NotNull
    private TripDirection direction;

    // Links to the RouteAssignment.id this trip was built from.
    // Example: "67b21125dc3f7d0044556677"
    private String routeAssignmentDocsId;

    // Links to TransportVehicle.id, copied in when the trip was made so it still
    // reads correctly after the assignment changes.
    // Example: "67b21123dc3f7d0022334455"
    @NotBlank
    private String vehicleDocsId;

    // Links to TransportDriver.id, copied in the same way.
    // Example: "67b21124dc3f7d0033445566"
    @NotBlank
    private String transportDriverDocsId;

    // Links to Staff.id for the attendant on board, copied in the same way.
    // Example: "67aa15d9dc3f7d0066666666"
    private String attendantStaffDocsId;

    // Example: TripStatus.COMPLETED
    @NotNull
    @Builder.Default
    private TripStatus status = TripStatus.SCHEDULED;

    // When the bus was meant to set off. Example: 2026-08-18T01:15:00Z
    private Instant plannedStartAt;

    // When it actually set off. Example: 2026-08-18T01:22:00Z
    private Instant actualStartAt;

    // When it finished the run. Example: 2026-08-18T02:10:00Z
    private Instant actualEndAt;

    // How many children were expected, from the allocations that applied that
    // day. Example: 38
    @NotNull
    @Builder.Default
    private Integer expectedStudentCount = 0;

    // How many actually got on. Example: 36
    @NotNull
    @Builder.Default
    private Integer boardedStudentCount = 0;

    // How many got off again. Example: 36
    @NotNull
    @Builder.Default
    private Integer completedStudentCount = 0;

    // Needed when the status is CANCELLED.
    // Example: "Route flooded after heavy rain; parents told to collect."
    private String cancellationReason;

    // Example: "Left ten minutes late because of a road closure."
    private String remarks;
}
