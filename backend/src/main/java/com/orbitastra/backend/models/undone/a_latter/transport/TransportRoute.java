package com.orbitastra.backend.models.undone.a_latter.transport;


import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "transport_routes")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransportRoute extends SchoolBase {

    //  TransportRoute
    //       │
    //       │ 1
    //       ▼
    //  RouteStop
    //       │
    //       │ uses
    //       ▼
    //  GeoLocation

    /**
     * Route code.
     *
     * Example:
     * R001
     */
    @Indexed(unique = true)
    private String routeCode;

    /**
     * Route name.
     *
     * Example:
     * East City Route
     */
    @Indexed
    private String routeName;

    /**
     * Route starting point.
     */
    private String startingLocation;

    /**
     * Route ending point.
     */
    private String endingLocation;

    /**
     * Approximate distance.
     */
    private Double estimatedDistanceKm;

    /**
     * Approximate duration.
     */
    private Integer estimatedDurationMinutes;

    /**
     * Whether route is active.
     */
    @Indexed
    private Boolean active;

    /**
     * Additional notes.
     */
    private String remarks;

}