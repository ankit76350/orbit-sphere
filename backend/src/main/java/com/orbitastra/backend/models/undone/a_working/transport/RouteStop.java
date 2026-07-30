package com.orbitastra.backend.models.undone.a_working.transport;


import java.time.LocalTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.transport.embedded.GeoLocation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "route_stops")
@CompoundIndex(
        name = "route_sequence_idx",
        def = "{'routeDocsId':1,'sequenceNumber':1}",
        unique = true
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RouteStop extends SchoolBase {

    /**
     * Parent route.
     */
    @Indexed
    private String routeDocsId;

    /**
     * Stop name.
     */
    private String stopName;

    /**
     * GPS location.
     */
    private GeoLocation location;

    /**
     * Stop order.
     */
    @Indexed
    private Integer sequenceNumber;

    /**
     * Expected pickup time.
     */
    private LocalTime pickupTime;

    /**
     * Expected drop time.
     */
    private LocalTime dropTime;

    /**
     * Geofence radius.
     *
     * Unit: meters
     */
    private Integer radiusMeters;

    /**
     * Whether stop is active.
     */
    @Indexed
    private Boolean active;

    /**
     * Additional remarks.
     */
    private String remarks;

}
