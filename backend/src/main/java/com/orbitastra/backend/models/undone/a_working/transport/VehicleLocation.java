package com.orbitastra.backend.models.undone.a_working.transport;



import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.transport.embedded.GeoLocation;
import com.orbitastra.backend.models.undone.a_working.transport.enums.TripStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Stores the latest GPS location of a vehicle.
 *
 * Updated continuously from the Driver App.
 *
 * Example:
 *
 * Every 5-10 seconds:
 *
 * Driver App
 *      ↓
 * Spring Boot API
 *      ↓
 * Update VehicleLocation
 *
 * Parents always read this collection
 * for real-time bus tracking.
 */
@Document(collection = "vehicle_locations")
@CompoundIndex(
        name = "vehicle_location_idx",
        def = "{'vehicleDocsId':1}",
        unique = true
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleLocation extends SchoolBase {

//     TransportVehicle
//        │
//        │
//        ▼
//     VehicleLocation
//        │
//        ├────────► RouteAssignment
//        │
//        ├────────► TransportRoute
//        │
//        └────────► Driver

// we will use the wescoket for the live tracing.....
// For the historical data we will use the mongodb database....

    /**
     * Vehicle being tracked.
     */
    @Indexed
    private String vehicleDocsId;

    /**
     * Current route assignment.
     *
     * Helps identify which
     * driver and route are active.
     */
    @Indexed
    private String routeAssignmentDocsId;

    /**
     * Current route.
     */
    @Indexed
    private String routeDocsId;

    /**
     * Current driver.
     */
    @Indexed
    private String driverDocsId;

    /**
     * Current GPS location.
     */
    private GeoLocation location;

    /**
     * Vehicle speed.
     *
     * Unit: km/h
     */
    private Double speed;

    /**
     * Heading.
     *
     * Unit: Degrees
     *
     * Example:
     * 0
     * 90
     * 180
     * 270
     */
    private Double heading;

    /**
     * GPS accuracy.
     *
     * Unit: meters
     */
    private Double accuracy;

    /**
     * Altitude.
     *
     * Unit: meters
     */
    private Double altitude;

    /**
     * GPS signal available.
     */
    @Builder.Default
    private Boolean gpsOnline = true;

    /**
     * Vehicle ignition.
     */
    @Builder.Default
    private Boolean ignitionOn = false;

    /**
     * Whether vehicle is moving.
     */
    @Builder.Default
    private Boolean moving = false;

    /**
     * Current trip status.
     */
    private TripStatus tripStatus;

    /**
     * Mobile battery percentage.
     *
     * Driver App only.
     */
    private Integer batteryLevel;

    /**
     * Mobile network.
     *
     * Example:
     * 4G
     * 5G
     * WiFi
     */
    private String networkType;

    /**
     * Last GPS update.
     */
    @Indexed
    private LocalDateTime lastUpdatedAt;

}