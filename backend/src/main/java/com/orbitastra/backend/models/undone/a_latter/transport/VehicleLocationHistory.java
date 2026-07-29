package com.orbitastra.backend.models.undone.a_latter.transport;



import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_latter.transport.embedded.GeoLocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Historical GPS locations of a vehicle.
 *
 * Unlike VehicleLocation, this collection stores
 * every recorded GPS point for analytics,
 * trip replay and route history.
 *
 * Example:
 *
 * 08:00  School
 * 08:05  MG Road
 * 08:10  Camp
 * 08:15  Swargate
 * 08:20  Katraj
 *
 * Recommended recording interval:
 *
 * • Every 30 seconds
 * OR
 * • Every 100 meters
 *
 * Parents DO NOT read this collection.
 *
 * Only:
 *
 * • Admin
 * • Reports
 * • Trip Replay
 * • Analytics
 */
@Document(collection = "vehicle_location_history")
@CompoundIndex(
        name = "vehicle_history_idx",
        def = "{'vehicleDocsId':1,'recordedAt':-1}"
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleLocationHistory extends SchoolBase {

//     TransportVehicle
//         │
//         ▼
//     VehicleLocationHistory
//         │
//         ├────────► RouteAssignment
//         │
//         ├────────► Driver
//         │
//         └────────► TransportRoute

    /**
     * Vehicle.
     */
    @Indexed
    private String vehicleDocsId;

    /**
     * Route Assignment.
     */
    @Indexed
    private String routeAssignmentDocsId;

    /**
     * Route.
     */
    @Indexed
    private String routeDocsId;

    /**
     * Driver.
     */
    @Indexed
    private String driverDocsId;

    /**
     * GPS coordinates.
     */
    private GeoLocation location;

    /**
     * Vehicle speed.
     *
     * Unit:
     * km/h
     */
    private Double speed;

    /**
     * Heading.
     *
     * Degrees.
     */
    private Double heading;

    /**
     * GPS accuracy.
     *
     * Meters.
     */
    private Double accuracy;

    /**
     * Altitude.
     *
     * Meters.
     */
    private Double altitude;

    /**
     * Ignition status.
     */
    @Builder.Default
    private Boolean ignitionOn = false;

    /**
     * Vehicle moving.
     */
    @Builder.Default
    private Boolean moving = false;

    /**
     * Mobile battery.
     */
    private Integer batteryLevel;

    /**
     * Mobile network.
     */
    private String networkType;

    /**
     * GPS timestamp.
     *
     * Sent from Driver App.
     */
    @Indexed
    private LocalDateTime recordedAt;

}